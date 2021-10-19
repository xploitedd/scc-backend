package pt.unl.fct.scc.sccbackend

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController("/media")
class SampleController {

    @GetMapping("/identity/{str}")
    fun exampleGetHandler(
        @PathVariable("str") string: String
    ): ResponseEntity<String> {
        // 200 - OK, 400 - Bad Request
        if (string.length <= 3)
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(string) // 200 - {string}
    }

}