package com.habit.agent.service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.entity.jpa.HabitGoalRecord;
import com.habit.agent.repository.jpa.HabitGoalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 自定义目标打卡记录业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HabitGoalRecordService {

    private final HabitGoalRecordRepository repository;

    /**
     * 录入或更新单个自定义目标打卡记录
     */
    @Transactional
    public HabitGoalRecordVO saveOrUpdate(HabitGoalRecord record) {
        if (record.getUserId() == null) {
            record.setUserId(AgentConstants.DEFAULT_USER_ID);
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }

        Optional<HabitGoalRecord> existing = repository
                .findByUserIdAndGoalIdAndRecordDate(
                        record.getUserId(), record.getGoalId(), record.getRecordDate());

        HabitGoalRecord saved;
        if (existing.isPresent()) {
            HabitGoalRecord db = existing.get();
            db.setValue(record.getValue());
            db.setRemark(record.getRemark());
            saved = repository.save(db);
            log.info("更新自定义目标记录: goalId={}, date={}", record.getGoalId(), record.getRecordDate());
        } else {
            saved = repository.save(record);
            log.info("新增自定义目标记录: goalId={}, date={}", record.getGoalId(), record.getRecordDate());
        }
        return toVO(saved);
    }

    /**
     * 按用户和日期查询所有自定义目标记录
     */
    @Transactional(readOnly = true)
    public List<HabitGoalRecordVO> getByDate(Long userId, LocalDate recordDate) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        return repository.findByUserIdAndRecordDate(userId, recordDate)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 按用户和精确日期范围查询自定义目标记录（升序，便于前端按日期聚合）
     */
    @Transactional(readOnly = true)
    public List<HabitGoalRecordVO> getByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        if (startDate == null) startDate = LocalDate.now().minusDays(29);
        if (endDate == null) endDate = LocalDate.now();
        return repository.findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(userId, startDate, endDate)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 查询最近 N 天的自定义目标记录
     */
    @Transactional(readOnly = true)
    public List<HabitGoalRecordVO> getRecent(Long userId, int days) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        return repository.findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(userId, startDate, endDate)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 按目标 ID 查询最近 N 天记录
     */
    @Transactional(readOnly = true)
    public List<HabitGoalRecordVO> getByGoalIdRecent(Long goalId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        return repository.findByGoalIdAndRecordDateBetweenOrderByRecordDateDesc(goalId, startDate, endDate)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 计算某目标最近 N 天的平均值
     */
    @Transactional(readOnly = true)
    public BigDecimal calcAverage(Long goalId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        var records = repository.findByGoalIdAndRecordDateBetweenOrderByRecordDateDesc(goalId, startDate, endDate);
        if (records.isEmpty()) return null;
        double sum = records.stream()
                .filter(r -> r.getValue() != null)
                .mapToDouble(r -> r.getValue().doubleValue())
                .sum();
        long validCount = records.stream().filter(r -> r.getValue() != null).count();
        if (validCount == 0) return null;
        return BigDecimal.valueOf(sum / validCount).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 删除目标相关的所有记录
     */
    @Transactional
    public void deleteByGoalId(Long goalId) {
        repository.findByGoalIdOrderByRecordDateDesc(goalId)
                .forEach(r -> repository.deleteById(r.getId()));
        log.info("删除目标的所有记录: goalId={}", goalId);
    }

    private HabitGoalRecordVO toVO(HabitGoalRecord entity) {
        return HabitGoalRecordVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .goalId(entity.getGoalId())
                .goalType(entity.getGoalType())
                .recordDate(entity.getRecordDate())
                .value(entity.getValue())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
