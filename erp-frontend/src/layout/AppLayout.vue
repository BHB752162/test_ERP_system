<template>
  <el-container style="height: 100vh">
    <Sidebar />
    <el-container>
      <Navbar />
      <el-main style="background: #f5f7fa; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>
