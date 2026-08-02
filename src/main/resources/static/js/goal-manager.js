/**
 * goal-manager.js — 目标管理公共模块
 *
 * 功能:
 *  1. 基于 HSL hash 的不重复颜色生成（用于自定义目标视觉标识）
 *  2. 目标 API 封装（CRUD）
 *  3. 内置目标元数据映射（类型名 → 显示名/图标/颜色）
 *  4. 通用 DOM 操作辅助
 */

// ===== 内置目标元数据 =====
const BUILTIN_GOAL_META = {
    'SLEEP':    { name: '睡眠', icon: 'bi-moon-stars',    color: '#4f46e5', bgFrom: '#eef2ff', bgTo: '#fff' },
    'EXERCISE': { name: '运动', icon: 'bi-bicycle',       color: '#f59e0b', bgFrom: '#fff7ed', bgTo: '#fff' },
    'WATER':    { name: '饮水', icon: 'bi-droplet-half',  color: '#06b6d4', bgFrom: '#ecfeff', bgTo: '#fff' },
    'DIET':     { name: '饮食', icon: 'bi-cup-hot',       color: '#db2777', bgFrom: '#faf5ff', bgTo: '#fff' }
};

// 已分配颜色池（用于确保不重复）
const _usedColors = new Set();

/**
 * 基于字符串 hash 生成 HSL 颜色
 * 保证同一标识始终返回相同颜色，且尽量不重复
 */
function generateColor(key) {
    if (!key) return { h: 200, s: 70, l: 50 };
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
        hash = ((hash << 5) - hash + key.charCodeAt(i)) | 0;
    }
    const h = Math.abs(hash % 360);
    // 使用饱和度和亮度的变化增加多样性
    const s = 60 + Math.abs(hash % 25);
    const l = 45 + Math.abs(hash % 15);
    return { h, s, l };
}

/**
 * 将 HSL 转为 CSS 颜色字符串
 */
function hsl(h, s, l, a = 1) {
    return `hsla(${h}, ${s}%, ${l}%, ${a})`;
}

/**
 * 为自定义目标获取独立颜色方案
 */
function getCustomGoalPalette(goalKey) {
    const { h, s, l } = generateColor(goalKey);
    return {
        color: hsl(h, s, l),
        colorLight: hsl(h, s, l + 25),
        bgFrom: hsl(h, s, l + 35, 0.2),
        bgTo: '#fff',
        gradient: `linear-gradient(135deg, ${hsl(h, s, l + 20, 0.3)}, #fff)`,
        gradient90: `linear-gradient(90deg, ${hsl(h, s, l)}, ${hsl(h, s, l + 15)})`
    };
}

// ===== API 封装 =====
const GoalAPI = {
    async list() {
        const res = await fetch('/api/goals');
        const data = await res.json();
        return data.code === 200 ? data.data : [];
    },

    async create(goal) {
        const res = await fetch('/api/goals', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(goal)
        });
        return res.json();
    },

    async update(id, goal) {
        const res = await fetch(`/api/goals/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(goal)
        });
        return res.json();
    },

    async remove(id) {
        const res = await fetch(`/api/goals/${id}`, { method: 'DELETE' });
        return res.json();
    },

    async saveRecord(record) {
        const res = await fetch('/api/goal-records/records', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(record)
        });
        return res.json();
    },

    async getRecords(days = 30) {
        const res = await fetch(`/api/goal-records/records/recent/${days}`);
        const data = await res.json();
        return data.code === 200 ? data.data : [];
    }
};

// ===== 辅助函数 =====
const GoalUtils = {
    /** 获取目标显示名 */
    getDisplayName(goal) {
        if (goal.goalType === 'CUSTOM' && goal.customName) return goal.customName;
        return BUILTIN_GOAL_META[goal.goalType]?.name || goal.goalType || '未知目标';
    },

    /** 获取目标图标 */
    getIcon(goal) {
        if (goal.goalType === 'CUSTOM') return 'bi-star';
        return BUILTIN_GOAL_META[goal.goalType]?.icon || 'bi-circle';
    },

    /** 获取目标颜色方案 */
    getPalette(goal) {
        if (goal.goalType === 'CUSTOM') {
            const key = goal.customName || goal.id || 'custom';
            return getCustomGoalPalette(key);
        }
        const meta = BUILTIN_GOAL_META[goal.goalType];
        if (meta) {
            return {
                color: meta.color,
                colorLight: meta.color,
                bgFrom: meta.bgFrom,
                bgTo: meta.bgTo,
                gradient: `linear-gradient(135deg, ${meta.bgFrom}, #fff)`,
                gradient90: `linear-gradient(90deg, ${meta.color}, ${meta.color}cc)`
            };
        }
        return getCustomGoalPalette(goal.goalType || 'unknown');
    },

    /** 获取目标类型中文名 */
    getTypeName(goalType) {
        return BUILTIN_GOAL_META[goalType]?.name || goalType || '';
    },

    /** 格式化周平均显示 */
    formatWeekAvg(goal, avgVal) {
        if (avgVal == null || avgVal === undefined) return '—';
        const unit = goal.unit || '';
        return `${avgVal}${unit}`;
    },

    /** 周期中文名 */
    periodName(period) {
        return { DAILY: '天', WEEKLY: '周', MONTHLY: '月' }[period] || period || '';
    }
};

// 全局可用
window.GoalAPI = GoalAPI;
window.GoalUtils = GoalUtils;
window.BUILTIN_GOAL_META = BUILTIN_GOAL_META;
window.generateColor = generateColor;
window.getCustomGoalPalette = getCustomGoalPalette;
