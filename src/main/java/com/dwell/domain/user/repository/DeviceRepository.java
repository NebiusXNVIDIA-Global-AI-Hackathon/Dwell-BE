package com.dwell.domain.user.repository;

import com.dwell.domain.user.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}
