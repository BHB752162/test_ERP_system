<template>
  <el-container style="height: 100vh">
    <Sidebar />
    <el-container style="display: flex; flex-direction: column">
      <Navbar />
      <el-main style="background: #f0f2f5; padding: 20px; overflow-y: auto">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>
