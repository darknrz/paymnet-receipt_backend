package com.paymentReceipt.repository;

import com.paymentReceipt.model.entity.Comprobante;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, String> {

    // Buscar por RUC emisor
    List<Comprobante> findByEmisorRucContainingIgnoreCase(String ruc);

    // Buscar por RUC/DNI receptor
    List<Comprobante> findByReceptorRucDniContainingIgnoreCase(String rucDni);

    // Buscar por tipo
    List<Comprobante> findByTipo(TipoComprobante tipo);

    // Buscar por estado
    List<Comprobante> findByEstado(EstadoComprobante estado);

    // Buscar por rango de fechas
    List<Comprobante> findByFechaEmisionBetween(LocalDate desde, LocalDate hasta);

    // Buscar por serie y número (identificador único del comprobante)
    Optional<Comprobante> findBySerieAndNumero(String serie, String numero);

    // Buscar por razón social del emisor
    List<Comprobante> findByEmisorRazonSocialContainingIgnoreCase(String razonSocial);

    // Total facturado
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Comprobante c WHERE c.estado = :estado")
    BigDecimal sumTotalByEstado(@Param("estado") EstadoComprobante estado);

    // Promedio total
    @Query("SELECT COALESCE(AVG(c.total), 0) FROM Comprobante c")
    BigDecimal avgTotal();

    // Conteo por tipo
    @Query("SELECT c.tipo, COUNT(c) FROM Comprobante c GROUP BY c.tipo")
    List<Object[]> countByTipo();

    // Conteo por moneda
    @Query("SELECT c.moneda, COUNT(c) FROM Comprobante c GROUP BY c.moneda")
    List<Object[]> countByMoneda();

    // Buscar texto libre en emisor o receptor
    @Query("""
            SELECT c FROM Comprobante c 
            WHERE LOWER(c.emisorRazonSocial) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.receptorNombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.emisorRuc) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.serie) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY c.createdAt DESC
            """)
    List<Comprobante> buscarTextoLibre(@Param("texto") String texto);
}
