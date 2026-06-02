package com.paymentReceipt.model.entity;

import com.paymentReceipt.model.enums.RolMensaje;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_chat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "comprobante")
@ToString(exclude = "comprobante")
public class MensajeChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolMensaje rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    // Nullable: solo se asocia cuando el mensaje analiza un comprobante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id")
    private Comprobante comprobante;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
