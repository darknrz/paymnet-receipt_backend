package com.paymentReceipt.repository;

import com.paymentReceipt.model.entity.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, String> {

    // Últimos N mensajes para contexto del chat
    List<MensajeChat> findTop20ByOrderByCreatedAtDesc();

    // Mensajes asociados a un comprobante
    List<MensajeChat> findByComprobanteIdOrderByCreatedAtAsc(String comprobanteId);
}
