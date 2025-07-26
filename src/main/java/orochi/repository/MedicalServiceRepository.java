package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.MedicalService;

import java.util.List;
import java.util.Map;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer> {

    /**
     * Get services by specialization ID
     */
    @Query("SELECT s FROM MedicalService s WHERE s.specId = :specId")
    List<MedicalService> findBySpecId(@Param("specId") Integer specId);

    /**
     * Get services with specialization data for invoice display
     * Returns Service ID, Service Name, Symptom, Price from Service and Specialization tables
     */
    @Query(value = """
        SELECT 
            s.ServiceID as serviceId,
            s.ServiceName as serviceName,
            sp.Symptom as symptom,
            s.Price as price,
            sp.SpecID as specId,
            sp.SpecName as specName,
            1 as quantity
        FROM Service s
        INNER JOIN Specialization sp ON s.SpecID = sp.SpecID
        WHERE s.SpecID = :specId
        """, nativeQuery = true)
    List<Map<String, Object>> getServicesWithSpecializationBySpecId(@Param("specId") Integer specId);

    /**
     * Get all services with specialization data for general consultation
     */
    @Query(value = """
        SELECT 
            s.ServiceID as serviceId,
            s.ServiceName as serviceName,
            sp.Symptom as symptom,
            s.Price as price,
            sp.SpecID as specId,
            sp.SpecName as specName,
            1 as quantity
        FROM Service s
        INNER JOIN Specialization sp ON s.SpecID = sp.SpecID
        ORDER BY s.ServiceID
        """, nativeQuery = true)
    List<Map<String, Object>> getAllServicesWithSpecialization();

    /**
     * Get default general consultation service
     */
    @Query(value = """
        SELECT TOP 1
            s.ServiceID as serviceId,
            s.ServiceName as serviceName,
            sp.Symptom as symptom,
            s.Price as price,
            sp.SpecID as specId,
            sp.SpecName as specName,
            1 as quantity
        FROM Service s
        INNER JOIN Specialization sp ON s.SpecID = sp.SpecID
        WHERE s.ServiceName LIKE '%General%' OR s.ServiceName LIKE '%Consultation%'
        ORDER BY s.ServiceID
        """, nativeQuery = true)
    List<Map<String, Object>> getDefaultGeneralConsultationService();

    /**
     * Count services by name - for quantity calculation
     */
    @Query("SELECT COUNT(s) FROM MedicalService s WHERE s.serviceName = :serviceName")
    Long countByServiceName(@Param("serviceName") String serviceName);
}

