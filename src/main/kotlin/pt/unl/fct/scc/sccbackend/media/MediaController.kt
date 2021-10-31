package pt.unl.fct.scc.sccbackend.media

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.storage.BlobInfo
import pt.unl.fct.scc.sccbackend.media.repo.MediaRepository
import pt.unl.fct.scc.sccbackend.users.model.User
import javax.servlet.http.HttpServletRequest

private val ALLOWED_MEDIA_TYPES: Set<String> = setOf(
    MediaType.IMAGE_JPEG_VALUE,
    MediaType.IMAGE_PNG_VALUE,
    MediaType.IMAGE_GIF_VALUE
)

@RestController
class MediaController(val repo: MediaRepository) {

    @PostMapping(MediaUri.MEDIAS)
    suspend fun uploadMedia(
        req: HttpServletRequest,
        user: User,
        @RequestBody data: ByteArray
    ): ResponseEntity<Any> {
        val contentType = req.contentType
        if (!ALLOWED_MEDIA_TYPES.contains(contentType))
            throw BadRequestException("The media type $contentType is not allowed")

        repo.createMedia(BlobInfo(
            data,
            contentType
        ))

        return ResponseEntity.ok().build()
    }

    @GetMapping(MediaUri.MEDIA)
    suspend fun downloadMedia(
        @PathVariable blobName: String
    ): ResponseEntity<ByteArray> {
        val info = repo.getMedia(blobName)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, info.contentType)
            .body(info.data)
    }

}