package com.maro.server

import io.minio.GetPresignedObjectUrlArgs
import io.minio.Http.Method
import io.minio.MinioClient
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.maro.server.media")

/** Where the media bucket is. All of it comes from the environment; the keys never go into git or the image. */
data class S3Config(
    /** e.g. `https://s3.eu-central-003.backblazeb2.com` */
    val endpoint: String,
    /** e.g. `eu-central-003`: given explicitly, so signing never has to ask the storage for it. */
    val region: String,
    val bucket: String,
    val accessKeyId: String,
    val secretAccessKey: String,
) {
    companion object {
        const val ENDPOINT_ENV = "S3_ENDPOINT"
        const val REGION_ENV = "S3_REGION"
        const val BUCKET_ENV = "S3_BUCKET"
        const val ACCESS_KEY_ENV = "S3_ACCESS_KEY_ID"
        const val SECRET_KEY_ENV = "S3_SECRET_ACCESS_KEY"

        /** `null` when any of the variables is missing: the server then runs without media (pushes still work). */
        fun from(env: (String) -> String?): S3Config? {
            fun value(name: String) = env(name)?.takeIf { it.isNotBlank() }
            return S3Config(
                endpoint = value(ENDPOINT_ENV) ?: return null,
                region = value(REGION_ENV) ?: return null,
                bucket = value(BUCKET_ENV) ?: return null,
                accessKeyId = value(ACCESS_KEY_ENV) ?: return null,
                secretAccessKey = value(SECRET_KEY_ENV) ?: return null,
            )
        }
    }
}

/** Presigned URLs (AWS signature v4) through the MinIO client: works with any S3-compatible storage. */
class S3MediaStore(private val config: S3Config) : MediaStore {

    private val client: MinioClient = MinioClient.builder()
        .endpoint(config.endpoint)
        .region(config.region)
        .credentials(config.accessKeyId, config.secretAccessKey)
        .build()

    init {
        // The endpoint and bucket only: the keys are never logged.
        log.info("media storage: {} bucket={}", config.endpoint, config.bucket)
    }

    override fun uploadUrl(key: String): String = presign(Method.PUT, key)

    override fun downloadUrl(key: String): String = presign(Method.GET, key)

    // Signing is local (no request to the storage): the region is known, so nothing blocks here.
    private fun presign(method: Method, key: String): String = client.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(method)
            .bucket(config.bucket)
            .`object`(key)
            .expiry(URL_VALIDITY_MINUTES, TimeUnit.MINUTES)
            .build(),
    )

    private companion object {
        /** Long enough to upload a photo on a slow network, short enough not to matter if a URL leaks. */
        const val URL_VALIDITY_MINUTES = 15
    }
}
