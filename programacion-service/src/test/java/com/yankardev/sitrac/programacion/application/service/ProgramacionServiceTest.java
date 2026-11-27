
package com.yankardev.sitrac.programacion.application.service;

import com.yankardev.sitrac.programacion.application.exception.ReglaNegocioException;
import com.yankardev.sitrac.programacion.domain.model.EstadoProgramacion;
import com.yankardev.sitrac.programacion.domain.model.Programacion;
import com.yankardev.sitrac.programacion.domain.port.out.NotificacionEventPort;
import com.yankardev.sitrac.programacion.domain.port.out.OperacionConsultaPort;
import com.yankardev.sitrac.programacion.domain.port.out.ProgramacionRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgramacionServiceTest {

    private ProgramacionRepositoryPort repository;
    private OperacionConsultaPort consulta;
    private NotificacionEventPort notificacion;
    private ProgramacionService service;

    @BeforeEach
    void configurar() {
        repository = mock(ProgramacionRepositoryPort.class);
        consulta = mock(OperacionConsultaPort.class);
        notificacion = mock(NotificacionEventPort.class);

        service = new ProgramacionService(repository, consulta, notificacion);
    }

    @Test
    void debeRechazarConductorConProgramacionActiva() {

        Programacion nueva = Programacion.builder()
                .pedidoId(1L)
                .conductorId(10L)
                .tractoId(20L)
                .semirremolqueId(30L)
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        when(repository.existeConductorConEstado(10L, EstadoProgramacion.PROGRAMADA
        )).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.crear(nueva)
        );

        assertEquals("El conductor ya tiene una programación activa", error.getMessage());
        verify(repository, never()).guardar(any());
        verifyNoInteractions(consulta, notificacion);
    }


    @Test
    void debeRechazarTractoConProgramacionActiva() {
        Programacion nueva = Programacion.builder()
                .pedidoId(1L)
                .conductorId(10L)
                .tractoId(20L)
                .semirremolqueId(30L)
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        when(repository.existeTractoConEstado(
                20L, EstadoProgramacion.PROGRAMADA
        )).thenReturn(true);

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> service.crear(nueva)
        );

        assertEquals(
                "El tracto ya tiene una programación activa",
                error.getMessage()
        );

        verify(repository, never()).guardar(any());
        verifyNoInteractions(consulta, notificacion);
    }

    @Test
    void debeRechazarSemirremolqueConProgramacionActiva() {
        Programacion nueva = Programacion.builder()
                .pedidoId(1L)
                .conductorId(10L)
                .tractoId(20L)
                .semirremolqueId(30L)
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        when(repository.existeSemirremolqueConEstado(
                30L, EstadoProgramacion.PROGRAMADA
        )).thenReturn(true);

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> service.crear(nueva)
        );

        assertEquals(
                "El semirremolque ya tiene una programación activa",
                error.getMessage()
        );

        verify(repository, never()).guardar(any());
        verifyNoInteractions(consulta, notificacion);
    }

    @Test
    void debeRechazarConductorConLicenciaVencida() {
        Programacion nueva = Programacion.builder()
                .pedidoId(1L)
                .conductorId(10L)
                .tractoId(20L)
                .semirremolqueId(30L)
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        when(consulta.buscarPedido(1L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.PedidoOperacion(
                                1L, "CEMENTO_BOLSA",
                                new java.math.BigDecimal("20"),
                                "REGISTRADO"
                        )
                ));

        when(consulta.buscarConductor(10L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.ConductorOperacion(
                                10L,
                                java.time.LocalDate.now().minusDays(1),
                                true,
                                true
                        )
                ));

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> service.crear(nueva)
        );

        assertEquals(
                "La licencia del conductor no está vigente",
                error.getMessage()
        );

        verify(repository, never()).guardar(any());
        verifyNoInteractions(notificacion);
    }


    @Test
    void debeCrearProgramacionCuandoTodosLosRecursosEstanDisponibles() {

        Programacion nueva = Programacion.builder()
                .pedidoId(1L)
                .conductorId(10L)
                .tractoId(20L)
                .semirremolqueId(30L)
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        // Simular un pedido registrado de 20 toneladas
        when(consulta.buscarPedido(1L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.PedidoOperacion(
                                1L,
                                "CEMENTO_BOLSA",
                                new java.math.BigDecimal("20"),
                                "REGISTRADO"
                        )
                ));

        // Simular conductor activo, disponible y con licencia vigente
        when(consulta.buscarConductor(10L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.ConductorOperacion(
                                10L,
                                java.time.LocalDate.now().plusMonths(6),
                                true,
                                true
                        )
                ));

        // Simular tracto disponible con capacidad suficiente
        when(consulta.buscarTracto(20L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.TractoOperacion(
                                20L,
                                new java.math.BigDecimal("35"),
                                "DISPONIBLE",
                                true
                        )
                ));

        // Simular semirremolque compatible y disponible
        when(consulta.buscarSemirremolque(30L))
                .thenReturn(java.util.Optional.of(
                        new OperacionConsultaPort.SemirremolqueOperacion(
                                30L,
                                "PLATAFORMA",
                                new java.math.BigDecimal("30"),
                                "DISPONIBLE",
                                true
                        )
                ));

        // Simular guardado en la base de datos
        when(repository.guardar(any(Programacion.class)))
                .thenAnswer(invocacion -> {
                    Programacion p = invocacion.getArgument(0);

                    return Programacion.builder()
                            .id(100L)
                            .pedidoId(p.getPedidoId())
                            .conductorId(p.getConductorId())
                            .tractoId(p.getTractoId())
                            .semirremolqueId(p.getSemirremolqueId())
                            .fechaProgramada(p.getFechaProgramada())
                            .estado(p.getEstado())
                            .build();
                });

        // Ejecutar la función real del servicio
        Programacion resultado = service.crear(nueva);

        // Verificar resultado
        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertEquals(EstadoProgramacion.PROGRAMADA, resultado.getEstado());

        // Verificar cambios de disponibilidad
        verify(consulta).cambiarEstadoPedido(1L, "PROGRAMADO");
        verify(consulta).cambiarDisponibilidadConductor(10L, false);
        verify(consulta).cambiarEstadoTracto(20L, "ASIGNADO");
        verify(consulta).cambiarEstadoSemirremolque(30L, "ASIGNADO");

        // Verificar publicación del evento
        verify(notificacion).programacionCreada(
                100L, 1L, nueva.getFechaProgramada()
        );
    }

}
