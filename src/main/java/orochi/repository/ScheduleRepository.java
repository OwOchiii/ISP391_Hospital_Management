package orochi.repository;

import orochi.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    @Query("SELECT s FROM Schedule s WHERE s.doctor.user.fullName LIKE %:keyword% OR s.scheduleDate = :date")
    List<Schedule> findByKeywordAndDate(String keyword, LocalDate date);

    List<Schedule> findByScheduleDate(LocalDate date);

    @Query("SELECT s FROM Schedule s WHERE s.scheduleDate BETWEEN :startDate AND :endDate")
    List<Schedule> findByDateRange(LocalDate startDate, LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE (s.doctor.user.fullName LIKE %:keyword%) AND (s.scheduleDate BETWEEN :startDate AND :endDate)")
    List<Schedule> findByKeywordAndDateRange(String keyword, LocalDate startDate, LocalDate endDate);
}