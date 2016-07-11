package com.rtx.uploads

import com.twitter.finatra._
import com.twitter.finatra.ContentType._

import com.amazonaws.services.{ s3 => aws }
import awscala._, s3._

import java.util.UUID.randomUUID

object App extends FinatraServer {

  class ExampleApp extends Controller {
    implicit val s3 = S3.at(Region0.US_EAST_1)

    case class UploadedFile(id: String, path: String)

    //generate id for new file
    def uuid = java.util.UUID.randomUUID.toString


    get("/") {
      request =>
      render.static("index.html").toFuture
    }

    post("/upload") { request =>
      val bucket: Bucket = Bucket("rtx-uploads")

      request.multiParams.get("file_upload") match {

        case Some(file) =>
          val key = uuid

          // Have to manually set metadata because we are reading straight from byte array
          var metadata = new aws.model.ObjectMetadata
          metadata.setContentLength(file.data.length);
          metadata.setContentType(file.contentType.get);

          val resp = s3.put(bucket=bucket, key=key, bytes= file.data, metadata=metadata)
          val s3obj: Option[S3Object] = bucket.getObject(key)

          s3obj match {
            case Some(obj) =>
              render.json(UploadedFile(key, obj.publicUrl.toString)).toFuture
            case None =>
              render.status(500).plain("bad request").toFuture
          }

        case None =>
          render.status(500).plain("no file").toFuture
      }

    }
  }
  register(new ExampleApp())
}
