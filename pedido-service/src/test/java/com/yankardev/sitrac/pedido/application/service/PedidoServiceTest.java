package com.yankardev.sitrac.pedido.application.service;

import com.yankardev.sitrac.pedido.application.exception.ReglaNegocioException;
import com.yankardev.sitrac.pedido.domain.model.EstadoPedido;
import com.yankardev.sitrac.pedido.domain.model.Pedido;
import com.yankardev.sitrac.pedido.domain.port.out.ClienteConsultaPort;
import com.yankardev.sitrac.pedido.domain.port.out.PedidoRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    private PedidoRepositoryPort repository;
    private ClienteConsultaPort clienteConsulta;
    private PedidoService service;

    @BeforeEach
    void configurar() {
        repository = mock(PedidoRepositoryPort.class);
        clienteConsulta = mock(ClienteConsultaPort.class);

        service = new PedidoService(repository, clienteConsulta);
    }

    private Pedido pedidoValido() {
        return Pedido.builder()
                .clienteId(1L)
                .toneladas(new BigDecimal("20"))
                .origen("Trujillo")
                .destino("Lima")
                .fechaSolicitud(LocalDate.now())
                .build();
    }

    @Test
    void debeRechazarOrigenYDestinoIguales() {
        Pedido pedido = pedidoValido();

        pedido = Pedido.builder()
                .clienteId(pedido.getClienteId())
                .toneladas(pedido.getToneladas())
                .origen("Trujillo")
                .destino(" TRUJILLO ")
                .fechaSolicitud(pedido.getFechaSolicitud())
                .build();

        Pedido pedidoFinal = pedido;

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> service.crear(pedidoFinal)
        );
        assertEquals("El origen y el destino no pueden ser iguales", error.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarOrigenVacio() {
        Pedido pedido = Pedido.builder()
                .clienteId(1L)
                .toneladas(new BigDecimal("20"))
                .origen("   ")
                .destino("Lima")
                .build();

        assertThrows(ReglaNegocioException.class, () -> service.crear(pedido));
    }

    @Test
    void debeRechazarToneladasEnCero() {
        Pedido pedido = Pedido.builder()
                .clienteId(1L)
                .toneladas(BigDecimal.ZERO)
                .origen("Trujillo")
                .destino("Lima")
                .build();

        assertThrows(ReglaNegocioException.class, () -> service.crear(pedido));
    }

    @Test
    void debeRegistrarPedidoValido() {
        Pedido pedido = pedidoValido();

        when(clienteConsulta.buscarCliente(1L))
                .thenReturn(Optional.of(
                        new ClienteConsultaPort.ClienteOperacion(1L, true)
                ));

        when(repository.guardar(any(Pedido.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Pedido resultado = service.crear(pedido);

        assertNotNull(resultado);
        assertEquals("Trujillo", resultado.getOrigen());
        assertEquals("Lima", resultado.getDestino());
        assertEquals(EstadoPedido.REGISTRADO, resultado.getEstado());

        verify(repository, times(1)).guardar(any(Pedido.class));
    }
}
