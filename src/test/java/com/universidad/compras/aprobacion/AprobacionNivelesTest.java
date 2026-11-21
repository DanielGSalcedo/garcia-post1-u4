package com.universidad.compras.aprobacion;

import com.universidad.compras.estado.FlujoSolicitud;
import com.universidad.compras.modelo.Solicitud;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AprobacionNivelesTest {
    @Autowired ServicioAprobacion servicio;
    @Autowired FlujoSolicitud flujo;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
            "1500000, Supervisor de Área",
            "2000000, Supervisor de Área",
            "2000001, Gerente de Área",
            "10000000, Gerente de Área",
            "10000001, Director Financiero"
    })
    void decidePorMontoIncluyendoLosLimites(double monto, String nivel) {
        var s = new Solicitud("S-001", "ana@example.com", monto, "SOFTWARE", "CC-100");
        assertEquals(nivel, servicio.evaluar(s).getNivelResolutor());
        assertEquals(nivel, s.getNivelResolutor());
        assertEquals("APROBADA", s.getEstado());
    }

    @Test
    void internacionalPasaPorCumplimientoYDespuesPorElNivelDeMonto() {
        var s = new Solicitud("S-003", "ana@example.com", 15_000_000, "INTERNACIONAL", "CC-300");
        var resultado = servicio.evaluar(s);
        assertTrue(resultado.isAprobada());
        assertEquals("Revisor de Cumplimiento Normativo", resultado.getNivelResolutor());
        assertTrue(resultado.getDetalle().contains("Director Financiero"));
    }

    @Test
    void unaRevisionInternacionalFallidaCortaLaCadena() {
        var cadena = new CadenaAprobacion(List.of(new RevisorCumplimiento(),
                (solicitud, siguiente) -> fail("No debió evaluar el monto")), flujo);
        var s = new Solicitud("S-004", "ana@example.com", 1_000_000, "INTERNACIONAL", " ");
        assertFalse(cadena.evaluar(s).isAprobada());
        assertEquals("RECHAZADA", s.getEstado());
    }

    @Test
    void seIncorporaUnNivelNuevoSinReescribirElServicio() {
        NivelAprobacion comprasEspeciales = (s, siguiente) ->
                new ResultadoAprobacion(true, "Comité especial", "Resuelta por un nivel nuevo");
        var cadena = new CadenaAprobacion(List.of(comprasEspeciales), flujo);
        var s = new Solicitud("S-005", "ana@example.com", 1, "SOFTWARE", "CC-300");
        assertEquals("Comité especial", cadena.evaluar(s).getNivelResolutor());
    }

    @Test
    void elControladorConservaSusRespuestasHttp() throws Exception {
        String payload = """
                {"id":"S-006","solicitanteEmail":"ana@example.com","monto":%s,
                 "categoria":"SOFTWARE","centroCosto":"CC-300"}
                """;
        mvc.perform(post("/api/solicitudes/evaluar").contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted("2500000")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nivelResolutor").value("Gerente de Área"));
        mvc.perform(post("/api/solicitudes/evaluar").contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted("-1")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.aprobada").value(false));
    }
}
