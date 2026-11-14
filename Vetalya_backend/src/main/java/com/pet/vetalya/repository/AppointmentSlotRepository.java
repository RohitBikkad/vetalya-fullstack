package com.pet.vetalya.repository;

import com.pet.vetalya.entity.AppointmentSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, String> {

    List<AppointmentSlot> findByVetUserIdAndSlotDateOrderByStartTimeAsc(String vetUserId, LocalDate slotDate);

    List<AppointmentSlot> findByHospitalIdAndSlotDateOrderByStartTimeAsc(String hospitalId, LocalDate slotDate);

    Optional<AppointmentSlot> findByVetUserIdAndSlotDateAndStartTime(String vetUserId, LocalDate slotDate, LocalTime startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AppointmentSlot s WHERE s.slotId = :slotId")
    Optional<AppointmentSlot> findByIdWithPessimisticLock(@Param("slotId") String slotId);

    @Query("SELECT s FROM AppointmentSlot s WHERE s.vetUserId = :vetUserId AND s.slotDate >= :fromDate AND s.slotDate <= :toDate AND s.status = 'AVAILABLE' ORDER BY s.slotDate ASC, s.startTime ASC")
    List<AppointmentSlot> findAvailableSlots(@Param("vetUserId") String vetUserId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
