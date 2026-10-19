package com.wacds.appointments.auth.membership;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessMembershipRepository
    extends JpaRepository<BusinessMembership, Long> {

    // =========================================================
    // SINGLE MEMBERSHIP · BY ID
    // =========================================================

    /**
     * Sobrescribimos el findById heredado de JpaRepository
     * para cargar explícitamente:
     *
     * - business
     * - user
     *
     * Esto es especialmente importante para operaciones de
     * gestión de miembros:
     *
     * - consultar miembro
     * - cambiar rol
     * - activar/desactivar membership
     *
     * De esta forma BusinessMembership puede utilizarse después
     * de salir del método transaccional sin provocar:
     *
     * LazyInitializationException
     */
    @Override
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    Optional<BusinessMembership> findById(
        Long id
    );

    // =========================================================
    // SINGLE MEMBERSHIP · BUSINESS + USER
    // =========================================================

    /**
     * Obtiene una membership concreta junto con:
     *
     * - business
     * - user
     *
     * La consulta mantiene disponibles ambas relaciones cuando
     * el objeto se utilice posteriormente en mappers o respuestas.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    Optional<BusinessMembership>
        findByBusinessIdAndUserId(
            Long businessId,
            Long userId
        );

    /**
     * Igual que findByBusinessIdAndUserId, pero únicamente
     * devuelve memberships activas.
     *
     * Utilizado principalmente para comprobar acceso al tenant
     * y validar el estado actual persistido de la sesión.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    Optional<BusinessMembership>
        findByBusinessIdAndUserIdAndActiveTrue(
            Long businessId,
            Long userId
        );

    // =========================================================
    // USER MEMBERSHIPS
    // =========================================================

    /**
     * Obtiene todas las memberships de un usuario.
     *
     * También carga business y user para que las entidades
     * devueltas sean seguras para su posterior transformación.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    List<BusinessMembership>
        findAllByUserIdOrderByCreatedAtAsc(
            Long userId
        );

    /**
     * Obtiene únicamente memberships activas del usuario.
     *
     * Se utiliza para autenticación, refresh y futura selección
     * de workspace/business.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    List<BusinessMembership>
        findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(
            Long userId
        );

    // =========================================================
    // BUSINESS MEMBERSHIPS
    // =========================================================

    /**
     * Devuelve todas las memberships pertenecientes al business.
     *
     * Incluye activas e inactivas.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    List<BusinessMembership>
        findAllByBusinessIdOrderByCreatedAtAsc(
            Long businessId
        );

    /**
     * Devuelve únicamente memberships activas del business.
     *
     * Es la consulta utilizada actualmente en el listado
     * principal del módulo Equipo.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    List<BusinessMembership>
        findAllByBusinessIdAndActiveTrueOrderByCreatedAtAsc(
            Long businessId
        );

    /**
     * Devuelve las memberships activas del business filtradas
     * por rol.
     */
    @EntityGraph(
        attributePaths = {
            "business",
            "user"
        }
    )
    List<BusinessMembership>
        findAllByBusinessIdAndRoleAndActiveTrueOrderByCreatedAtAsc(
            Long businessId,
            BusinessRole role
        );

    // =========================================================
    // EXISTS
    // =========================================================

    /**
     * Comprueba si existe una relación business/user,
     * independientemente de que esté activa.
     */
    boolean existsByBusinessIdAndUserId(
        Long businessId,
        Long userId
    );

    /**
     * Comprueba si existe una relación business/user activa.
     */
    boolean existsByBusinessIdAndUserIdAndActiveTrue(
        Long businessId,
        Long userId
    );
}