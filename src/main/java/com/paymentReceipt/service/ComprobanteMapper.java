package com.paymentReceipt.service;

import com.paymentReceipt.agent.ComprobanteAgentService.AgentResult;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.entity.Comprobante;
import com.paymentReceipt.model.entity.ItemComprobante;
import com.paymentReceipt.model.enums.EstadoComprobante;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComprobanteMapper {

    public ComprobanteResponse toResponse(Comprobante comprobante) {
        List<ComprobanteResponse.ItemResponse> items = comprobante.getItems().stream()
                .map(item -> new ComprobanteResponse.ItemResponse(
                        item.getId(),
                        item.getDescripcion(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                ))
                .toList();

        return new ComprobanteResponse(
                comprobante.getId(),
                comprobante.getTipo(),
                comprobante.getSerie(),
                comprobante.getNumero(),
                comprobante.getFechaEmision(),
                comprobante.getEmisorRuc(),
                comprobante.getEmisorRazonSocial(),
                comprobante.getEmisorDireccion(),
                comprobante.getReceptorRucDni(),
                comprobante.getReceptorNombre(),
                comprobante.getSubtotal(),
                comprobante.getIgv(),
                comprobante.getTotal(),
                comprobante.getMoneda(),
                comprobante.getEstado(),
                comprobante.getArchivoNombre(),
                comprobante.getObservaciones(),
                items,
                comprobante.getCreatedAt(),
                comprobante.getUpdatedAt()
        );
    }

    public Comprobante fromAgentResult(AgentResult result, String archivoNombre, String rawJson) {
        Comprobante comprobante = Comprobante.builder()
                .tipo(result.tipo())
                .serie(result.serie())
                .numero(result.numero())
                .fechaEmision(result.fechaEmision())
                .emisorRuc(result.emisorRuc())
                .emisorRazonSocial(result.emisorRazonSocial())
                .emisorDireccion(result.emisorDireccion())
                .receptorRucDni(result.receptorRucDni())
                .receptorNombre(result.receptorNombre())
                .subtotal(result.subtotal())
                .igv(result.igv())
                .total(result.total())
                .moneda(result.moneda() != null ? result.moneda() : "PEN")
                .estado(result.estado() != null ? result.estado() : EstadoComprobante.PENDIENTE)
                .observaciones(result.observaciones())
                .archivoNombre(archivoNombre)
                .rawAgentResponse(rawJson)
                .build();

        if (result.items() != null) {
            result.items().forEach(itemResult -> {
                ItemComprobante item = ItemComprobante.builder()
                        .descripcion(itemResult.descripcion())
                        .cantidad(itemResult.cantidad())
                        .precioUnitario(itemResult.precioUnitario())
                        .subtotal(itemResult.subtotal())
                        .build();
                comprobante.addItem(item);
            });
        }

        return comprobante;
    }
}
