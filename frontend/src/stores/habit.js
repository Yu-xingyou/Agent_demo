import { defineStore } from 'pinia'
import * as habitApi from '@/api/habit'

export const useHabitStore = defineStore('habit', {
  state: () => ({
    today: null,
    recent: [],
    list: [],
    loading: false,
  }),
  actions: {
    async loadToday() {
      this.loading = true
      try {
        this.today = await habitApi.getToday()
      } finally {
        this.loading = false
      }
    },
    async loadRecent(days = 7) {
      this.loading = true
      try {
        this.recent = await habitApi.getRecent(days)
      } finally {
        this.loading = false
      }
    },
    async loadRange(startDate, endDate) {
      this.loading = true
      try {
        this.list = await habitApi.listByRange(startDate, endDate)
      } finally {
        this.loading = false
      }
    },
  },
})
