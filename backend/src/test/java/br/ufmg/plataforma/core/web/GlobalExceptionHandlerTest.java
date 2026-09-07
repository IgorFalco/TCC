package br.ufmg.plataforma.core.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufmg.plataforma.core.domain.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void validacao_retorna_400_no_formato_problem_detail() throws Exception {
        mvc.perform(post("/dummy/validate").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://plataforma.ufmg.br/errors/validation"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void recurso_inexistente_retorna_404() throws Exception {
        mvc.perform(get("/dummy/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://plataforma.ufmg.br/errors/not-found"))
                .andExpect(jsonPath("$.detail").value("Widget 7 não encontrado"));
    }

    @Test
    void erro_inesperado_retorna_500_sem_vazar_detalhe() throws Exception {
        mvc.perform(get("/dummy/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://plataforma.ufmg.br/errors/internal"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @RestController
    static class DummyController {

        record Payload(@NotBlank String name) {}

        @PostMapping("/dummy/validate")
        void validate(@Valid @RequestBody Payload payload) {}

        @org.springframework.web.bind.annotation.GetMapping("/dummy/missing")
        void missing() {
            throw new ResourceNotFoundException("Widget", 7);
        }

        @org.springframework.web.bind.annotation.GetMapping("/dummy/boom")
        void boom() {
            throw new IllegalStateException("detalhe interno sensível");
        }
    }
}
