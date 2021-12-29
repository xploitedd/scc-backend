package pt.unl.fct.scc.sccbackend.common.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Service
class FsBlobStorage(
    val digester: MessageDigest
) : BlobStorage {

    companion object {
        private const val FS_PATH = "blobs/"
        private val log = LoggerFactory.getLogger(FsBlobStorage::class.java)

        private val folder: File by lazy {
            val folder = Paths.get(FS_PATH).toFile()
            if (!folder.exists())
                folder.mkdir()

            folder
        }
    }

    override suspend fun upload(blobName: String, info: BlobInfo) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val file = Paths.get(folder.absolutePath, blobName)
                if (!file.exists()) {
                    log.info("Uploading file $blobName to $file")
                    FileOutputStream(file.toFile()).use {
                        it.write(info.data)
                    }
                }
            }
        }
    }

    override suspend fun download(blobName: String): BlobInfo {
        val bytes = coroutineScope {
            async(Dispatchers.IO) {
                val file = Paths.get(folder.absolutePath, blobName)
                if (!file.exists())
                    throw NotFoundException()

                FileInputStream(file.toFile()).use {
                    it.readAllBytes()
                }
            }
        }.await()

        val hash = Base64.getUrlEncoder()
            .encodeToString(digester.digest(bytes))

        return BlobInfo(
            bytes,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            hash
        )
    }

    override suspend fun exists(blobName: String): Boolean {
        val file = Paths.get(folder.absolutePath, blobName)
        return file.exists()
    }

    override suspend fun delete(blobName: String) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val file = Paths.get(folder.absolutePath, blobName)
                file.deleteIfExists()
            }
        }
    }

}