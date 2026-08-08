package com.habit.agent.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.repository.jpa.HabitRecordRepository;
import com.habit.agent.service.HabitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯记录业务逻辑实现（子模块 2-2）
 *
 * 核心功能: saveOrUpdate 实现同一天重复打卡为更新（依赖 uk_user_date 唯一约束）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HabitServiceImpl implements HabitService {

    private final HabitRecordRepository habitRecordRepository;

    /**
     * 录入或更新打卡记录（同一天重复打卡为更新）
     *
     * @param record 打卡数据（userId + recordDate 用于判重）
     * @return 保存后的记录（含自动计算的 sleepDuration）
     */
    @Override
    @Transactional
    public HabitRecordVO saveOrUpdate(HabitRecord record) {
        if (record.getUserId() == null) {
            record.setUserId(AgentConstants.DEFAULT_USER_ID);
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }

        // 查询当天是否已有记录
        Optional<HabitRecord> existing = habitRecordRepository
                .findByUserIdAndRecordDate(record.getUserId(), record.getRecordDate());

        if (existing.isPresent()) {
            // 更新已有记录
            HabitRecord db = existing.get();
            db.setSleepTime(record.getSleepTime());
            db.setWakeTime(record.getWakeTime());
            db.setSleepQuality(record.getSleepQuality());
            db.setDietDesc(record.getDietDesc());
            db.setDietScore(record.getDietScore());
            db.setExerciseType(record.getExerciseType());
            db.setExerciseDuration(record.getExerciseDuration());
            db.setWaterIntake(record.getWaterIntake());
            db.setMood(record.getMood());
            db.setRemark(record.getRemark());
            db.calculateSleepDuration(); // 显式计算，确保 VO 拿到正确值
            HabitRecord saved = habitRecordRepository.save(db);
            log.info("更新打卡记录: userId={}, date={}, sleepDuration={}", saved.getUserId(), saved.getRecordDate(), saved.getSleepDuration());
            return toVO(saved);
        } else {
            // 新增记录
            record.calculateSleepDuration(); // 显式计算，确保 VO 拿到正确值
            HabitRecord saved = habitRecordRepository.save(record);
            log.info("新增打卡记录: userId={}, date={}, sleepDuration={}", saved.getUserId(), saved.getRecordDate(), saved.getSleepDuration());
            return toVO(saved);
        }
    }

    /**
     * 按日期范围查询记录
     *
     * @param userId    用户 id，可空（空时使用默认用户）
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该区间打卡记录视图列表（倒序）
     * @throws BusinessException 当开始日期晚于结束日期时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public List<HabitRecordVO> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(AgentConstants.CODE_DATE_RANGE_ERROR,
                    "开始日期不能晚于结束日期");
        }
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(userId, startDate, endDate)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询最近 N 天记录
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   天数
     * @return 最近 N 天打卡记录视图列表（倒序）
     */
    @Override
    @Transactional(readOnly = true)
    public List<HabitRecordVO> getRecentRecords(Long userId, int days) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        return getRecordsByDateRange(userId, startDate, endDate);
    }

    /**
     * 按日期查询记录
     *
     * @param userId      用户 id，可空（空时使用默认用户）
     * @param recordDate  指定日期
     * @return 该日打卡记录视图；无则 null
     */
    @Override
    @Transactional(readOnly = true)
    public HabitRecordVO getRecordByDate(Long userId, LocalDate recordDate) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitRecordRepository
                .findByUserIdAndRecordDate(userId, recordDate)
                .map(this::toVO)
                .orElse(null);
    }

    /**
     * 查询所有记录
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 全部打卡记录视图列表（倒序）
     */
    @Override
    @Transactional(readOnly = true)
    public List<HabitRecordVO> getAllRecords(Long userId) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitRecordRepository
                .findByUserIdOrderByRecordDateDesc(userId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询今日记录
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 今日打卡记录视图；无则 null
     */
    @Override
    @Transactional(readOnly = true)
    public HabitRecordVO getTodayRecord(Long userId) {
        return getRecordByDate(userId, LocalDate.now());
    }

    /**
     * 按 ID 查询记录
     *
     * @param id 记录 id
     * @return 打卡记录视图
     * @throws BusinessException 当记录不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public HabitRecordVO getRecordById(Long id) {
        return habitRecordRepository.findById(id)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_RECORD_NOT_FOUND,
                        "习惯记录不存在: id=" + id));
    }

    /**
     * 删除记录
     *
     * @param id 记录 id
     * @throws BusinessException 当记录不存在时抛出
     */
    @Override
    @Transactional
    public void deleteRecord(Long id) {
        if (!habitRecordRepository.existsById(id)) {
            throw new BusinessException(AgentConstants.CODE_RECORD_NOT_FOUND,
                    "习惯记录不存在: id=" + id);
        }
        habitRecordRepository.deleteById(id);
        log.info("删除打卡记录: id={}", id);
    }

    /**
     * Entity → VO 转换
     */
    private HabitRecordVO toVO(HabitRecord entity) {
        return HabitRecordVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .recordDate(entity.getRecordDate())
                .sleepTime(entity.getSleepTime())
                .wakeTime(entity.getWakeTime())
                .sleepDuration(entity.getSleepDuration())
                .sleepQuality(entity.getSleepQuality())
                .dietDesc(entity.getDietDesc())
                .dietScore(entity.getDietScore())
                .exerciseType(entity.getExerciseType())
                .exerciseDuration(entity.getExerciseDuration())
                .waterIntake(entity.getWaterIntake())
                .mood(entity.getMood())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
