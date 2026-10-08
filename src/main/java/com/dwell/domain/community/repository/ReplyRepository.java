package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
}
