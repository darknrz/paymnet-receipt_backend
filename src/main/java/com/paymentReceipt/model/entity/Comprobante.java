package com.paymentReceipt.model.entity;

import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comprobantes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "items")
@ToString(exclude = "items")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoComprobante tipo;

    @Column(name = "serie", length = 10)
    private String serie;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    // --- Emisor ---
    @Column(name = "emisor_ruc", length = 15)
    private String emisorRuc;

    @Column(name = "emisor_razon_social")
    private String emisorRazonSocial;

    @Column(name = "emisor_direccion", columnDefinition = "TEXT")
    private String emisorDireccion;

    // --- Receptor ---
    @Column(name = "receptor_ruc_dni", length = 15)
    private String receptorRucDni;

    @Column(name = "receptor_nombre")
    private String receptorNombre;

    // --- Totales ---
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "moneda", length = 3)
    @Builder.Default
    private String moneda = "PEN";

    // --- Estado y metadata ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoComprobante estado = EstadoComprobante.PENDIENTE;

    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // JSON raw devuelto por el agent (útil para debug/auditoría)
    @Column(name = "raw_agent_response", columnDefinition = "TEXT")
    private String rawAgentResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemComprobante> items = new ArrayList<>();

    // Helper para agregar items
    public void addItem(ItemComprobante item) {
        items.add(item);
        item.setComprobante(this);
    }
}
