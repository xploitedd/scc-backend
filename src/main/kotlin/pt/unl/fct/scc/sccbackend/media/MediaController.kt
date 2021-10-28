package pt.unl.fct.scc.sccbackend.media

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import pt.unl.fct.scc.sccbackend.media.model.Media
import pt.unl.fct.scc.sccbackend.media.repo.MediaRepository

@RestController
class MediaController(

    val repo: MediaRepository,

    ) {
    @PostMapping(MediaUri.MEDIAS)
    suspend fun uploadMedia(
        /** blobName should be done before storing media so it can be adressed in the message**/
        media : Media
    ): ResponseEntity<Any> {
        val createdMedia = repo.uploadMedia(media)
        return ResponseEntity.created(MediaUri.forMedia(createdMedia.blobName))
            .build()
    }


    @DeleteMapping(MediaUri.MEDIA)
    suspend fun deleteMEDIA(
        blobName: String
    ): ResponseEntity<Any> {

        repo.deleteMedia(blobName)
        return ResponseEntity.noContent()
            .build()
    }

    @GetMapping(MediaUri.MEDIA)
    suspend fun downloadMedia(
        blobName: String
    ): ResponseEntity<Any> {
        val content = repo.downloadMedia(blobName)
        return ResponseEntity.ok(content)
    }
}