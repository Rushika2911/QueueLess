package com.queueless.repository;

import com.queueless.entity.Appointment;
import com.queueless.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByProviderIdAndAppointmentDate(Long providerId, LocalDate appointmentDate);

    List<Appointment> findByProviderIdAndAppointmentDateAndStatusNot(Long providerId, LocalDate appointmentDate, AppointmentStatus status);

    Page<Appointment> findByCustomerId(Long customerId, Pageable pageable);

    Page<Appointment> findByCustomerIdAndStatus(Long customerId, AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByCustomerIdAndAppointmentDate(Long customerId, LocalDate appointmentDate, Pageable pageable);

    Page<Appointment> findByCustomerIdAndStatusAndAppointmentDate(Long customerId, AppointmentStatus status, LocalDate appointmentDate, Pageable pageable);

    Page<Appointment> findByProviderId(Long providerId, Pageable pageable);

    Page<Appointment> findByProviderIdAndStatus(Long providerId, AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByProviderIdAndAppointmentDate(Long providerId, LocalDate appointmentDate, Pageable pageable);

    Page<Appointment> findByProviderIdAndStatusAndAppointmentDate(Long providerId, AppointmentStatus status, LocalDate appointmentDate, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.provider.id = :providerId " +
           "AND a.appointmentDate = :date " +
           "AND a.status <> 'CANCELLED' " +
           "AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlappingAppointmentsForProvider(
            @Param("providerId") Long providerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("SELECT a FROM Appointment a WHERE a.customer.id = :customerId " +
           "AND a.appointmentDate = :date " +
           "AND a.status <> 'CANCELLED' " +
           "AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlappingAppointmentsForCustomer(
            @Param("customerId") Long customerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
