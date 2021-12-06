package com.jsoizo

import cats.effect._
import cats.effect.unsafe.implicits._
import com.jsoizo.api.ApiRoute
import io.circe
import sttp.tapir.serverless.aws.lambda._
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{Header, StatusCode}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

object LambdaApp extends App with ApiRoute {

  // ルータ = ビジネスロジックの宣言
  val options: AwsServerOptions[IO] = AwsCatsEffectServerOptions.default[IO].copy(encodeResponseBody = false)
  val route: Route[IO] = AwsCatsEffectServerInterpreter(options).toRoute(pingEndPoint)

  // 環境変数からAWS LambdaのRuntime APIのホスト名を取得
  val lambdaRuntimeApiHostName = Option {
    System.getenv("AWS_LAMBDA_RUNTIME_API")
  }.getOrElse("127.0.0.1:3001")

  // Runtime APIを呼び出して処理対象のリクエスト(Lambda呼び出しリクエスト)を取得する
  // APIのHTTPレスポンスはtapirのAwsRequest型へデコードする
  def callNextInvocationApi: IO[Response[Either[ResponseException[String, circe.Error], AwsRequest]]] =
    HttpClientFs2Backend.resource[IO]().use {
      basicRequest
        .get(uri"http://${lambdaRuntimeApiHostName}/2018-06-01/runtime/invocation/next")
        .response(asJson[AwsRequest])
        .send(_)
    }

  // Runtime APIを呼び出してリクエスト(Lambda呼び出しリクエスト)に対するレスポンスを送信する
  def callResponseApi(requestId: String, response: AwsResponse): IO[Response[Either[String, String]]] =
    HttpClientFs2Backend.resource[IO]().use {
      basicRequest
        .post(uri"http://${lambdaRuntimeApiHostName}/2018-06-01/runtime/invocation/${requestId}/response")
        .body(response.asJson)
        .contentType("application/json")
        .send(_)
    }

  // HTTPヘッダーからAWS LambdaのリクエストIDを取り出す
  def getAwsRequestId(headers: Seq[Header]): IO[String] = {
    val requestIdHeaderName = "lambda-runtime-aws-request-id"
    IO.fromOption(headers.find(_.name == requestIdHeaderName).map(_.value))(
      new RuntimeException(s"Cannot found ${requestIdHeaderName} header from request")
    )
  }

  // HTTPの処理 = ルータに対してLambdaのイベントを渡してビジネスロジックを呼び出す
  def handleHttpBody(awsRequest: AwsRequest): IO[AwsResponse] = for {
    _ <- IO.println(s"http request received. ${awsRequest.rawPath}")
    response <- route(awsRequest)
  } yield response

  // RuntimeApiのボディパースエラーをレスポンスに変換する
  def handleRuntimeApiBodyError(error: ResponseException[String, circe.Error]): IO[AwsResponse] = {
    val errorMessage = error match {
      case DeserializationException(body, e) => s"${e.getMessage} body: ${body}"
      case e                                 => e.getMessage
    }
    IO.pure(AwsResponse(Nil, isBase64Encoded = false, StatusCode.BadRequest.code, Map.empty, errorMessage))
  }

  while (true) {
    val io = for {
      // RuntimeApiを呼び出し次のイベントを取り出す
      nextInvocationApiResult <- callNextInvocationApi
      // RuntimeApiのレスポンスヘッダからLambdaリクエストのIDを取得
      requestId <- getAwsRequestId(nextInvocationApiResult.headers)
      // RuntimeApiのリクエストボディ=Lambdaイベントのパースに
      //  成功 => イベントをもとにルータ呼び出してレスポンスを返却
      //  失敗 => 失敗時のレスポンスを返却
      response <- nextInvocationApiResult.body match {
        case Right(awsRequest) => handleHttpBody(awsRequest)
        case Left(error)       => handleRuntimeApiBodyError(error)
      }
      // ルータから帰ってきた結果をRuntimeApiに返却
      responseApiResult <- callResponseApi(requestId, response)
      _ <- responseApiResult.body match {
        case Left(errorStr) => IO.raiseError(new RuntimeException(errorStr))
        case Right(_)       => IO.pure()
      }
    } yield ()
    io.handleErrorWith { error =>
      IO.println(s"lambda exec error: ${error.getMessage}")
    }.unsafeRunSync()
  }

}
