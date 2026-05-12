<script setup>
const handleOpen = (key, keyPath) => {
  console.log(key, keyPath)
}
const handleClose = (key, keyPath) => {
  console.log(key, keyPath)
}

</script>
<template>
  <div class="common-layout">
    <el-container>
      <el-aside width="200px">
      <div class="mb-2">
           <h5 >学生低碳管理系统</h5>
      </div>
      <div class="mt-3">
    <el-col :span="12">
      <el-menu
        active-text-color="#ffd04b"
        background-color="#1d5e36"
        class="el-menu-vertical-demo"
  
        text-color="#fff"
        @open="handleOpen"
        @close="handleClose"
        router="true"
      >
      <el-menu-item index="/index/summary">
          <span>首页</span>
        </el-menu-item>
         <el-menu-item index="/index/student/list">
          <span>学生信息列表</span>
        </el-menu-item>
        <el-menu-item index="/index/student/add">
          <span>学生用餐接口</span>
        </el-menu-item>
         <el-sub-menu index="/index/student/meal">
          <template #title>
            <span>学生餐饮记录表</span>
          </template>
              <el-menu-item index="/index/student/meal/daliy">
              <span>每天餐饮信息</span>
              </el-menu-item>
              <el-menu-item index="/index/student/meal/week">
              <span>每周餐饮信息</span>
              </el-menu-item>
              <el-menu-item index="/index/student/meal/month">
              <span>每月餐饮信息</span>
              </el-menu-item>
              <el-menu-item index="/index/student/meal/term">
              <span>每学期餐饮信息</span>
              </el-menu-item>
              <el-menu-item index="/index/student/meal/year">
              <span>每年餐饮信息</span>
              </el-menu-item>
        </el-sub-menu>
         <el-sub-menu index="/index/student/period">
          <template #title>
            <span>周期型碳排放折线图</span>
          </template>
              <el-menu-item index="/index/student/period/week">
              <span>每周碳排放信息</span>
              </el-menu-item>
              <el-menu-item index="/index/student/period/month">
              <span>每月碳排放信息</span>
              </el-menu-item>
        </el-sub-menu>
        
      </el-menu>
    </el-col>
      </div>

      </el-aside>
      <el-container>
        <el-header>
        
         </el-header>
        <el-main>
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
 .common-layout {
  width: 100%;
  height: 100vh;
}

.el-container {
  height: 100%;
}
.el-header {
  background-color:#fff; 
  color: #fff;
  line-height: 60px;
  text-align: center;
}
.el-aside {
background-color: #1d5e36;
  color: #333;
  text-align: center;
}
.mb-2{
  height:50px;
  margin-bottom: 5px;
  margin-top: 20px;
  font-size: 30px;

  color: white;
}
.el-menu-vertical-demo{
  margin-top: 20px;
  width: 200px;
}
.button-row {
float: right;
margin-top: 15px;
}
</style>