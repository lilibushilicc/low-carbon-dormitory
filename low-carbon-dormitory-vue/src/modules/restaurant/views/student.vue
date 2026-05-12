<script setup>
import { onMounted, ref, reactive } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import {studentPageService,studentAddService,studentDeleteService,studentFindService,studentUpdateService} from '../api/student.js'
//表单数据
const tableData = ref([])
//搜索表单数据
const searchForm  = ref({
  name: '',
  major: ''
})
const formLabelWidth = '100px'
//分页数据
const currentPage = ref(1)
const pageSize = ref(10)
const size = ref('default')
const background = ref(false)
const disabled = ref(false)
const dialogVisible11=ref(false)
const total = ref(0)
const handleSizeChange = (val) => {
pageSize.value  = val
currentPage.value = 1

}
const handleCurrentChange = (val) => {
  currentPage.value  = val
   page1()
}
//分页请求

const page1 = async () => {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      name: searchForm.value.name?.trim()||undefined,
      major: searchForm.value.major || undefined
    }
    console.log(params)
    const result = await studentPageService(params)
      tableData.value = result.data.list || result.data.records || result.data.data || []
      total.value = result.data.total || result.data.count || result.data.totalCount || 0
}
//重置搜索输入框
const resetForm = () => {
  searchForm.value = {
    name: '',
    gender: ''
  }
  currentPage.value = 1
   page1()
}
onMounted(() => {
  page1()
})


// 弹窗控制
const dialogVisible = ref(false)
const formRef = ref(false)
const loading = ref(false)
import {
  Delete,
  Edit,
} from '@element-plus/icons-vue'
import { pa } from 'element-plus/es/locale/index.mjs'
// 表单数据（和截图中的字段完全对应）
const formData = reactive({
  stuNum: '', // 学号
  name: '',      // 姓名
  gender: '1',   // 性别（1男/0女）
  idCard: '',    // 身份证号
  phone: '',     // 手机号
  college: '',   // 学院
  major: '',     // 专业
  className: '', // 班级
  grade: '',     // 年级
  dormBuilding: '', // 宿舍楼
  dormRoom: '',  // 宿舍号
  carbonScore: '',
  password: '' // 碳积分
})
const form = reactive({
  stuNum: '', // 学号
  name: '',      // 姓名
  gender: '1',   // 性别（1男/0女）
  idCard: '',    // 身份证号
  phone: '',     // 手机号
  college: '',   // 学院
  major: '',     // 专业
  className: '', // 班级
  grade: '',     // 年级
  dormBuilding: '', // 宿舍楼
  dormRoom: '',  // 宿舍号
  carbonScore: '',
  password: '' // 碳积分
})
// 表单校验规则
const rules = reactive({
  stuNum: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{8,12}$/, message: '学号格式不正确', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度在2-10个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  college: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  major: [
    { required: true, message: '请输入专业', trigger: 'blur' }
  ],
  className: [
    { required: true, message: '请输入班级', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请选择年级', trigger: 'change' }
  ],
  dormBuilding: [
    { required: true, message: '请输入宿舍楼', trigger: 'blur' }
  ],
  dormRoom: [
    { required: true, message: '请输入宿舍号', trigger: 'blur' }
  ]
})
const openAddDialog = () => { 
  dialogVisible.value = true
}

const submitForm = async () => { 
  const params = {
    stuNum: formData.stuNum,
    name: formData.name,
    gender: formData.gender,
    idCard: formData.idCard,
    phone: formData.phone,
    college: formData.college,
    major: formData.major,
    className: formData.className,
    grade: formData.grade,
    dormBuilding: formData.dormBuilding,
    dormRoom: formData.dormRoom,
    carbonScore: formData.carbonScore
  }
  const res = await studentAddService(params)
  if (res.code === 1) {
    ElMessage.success('新增成功')
    dialogVisible.value = false
    page1()
  } else {
    ElMessage.error('新增失败')
  }
}
const deleteStudent = async(studentId) => { 

  const params={
    studentId: studentId
  }
  const res = await studentDeleteService(params)
  if (res.code === 1) { 
    ElMessage.success('删除成功')
    page1()
  } else {
    ElMessage.error('删除失败')
  }
  }
const findByid=async(id)=>{
  const params={
    id: id
  }

  const res=await studentFindService(params)
  if(res.code===1){
    form.stuNum=res.data.stuNum
    form.name=res.data.name
    form.gender=res.data.gender
    form.idCard=res.data.idCard
    form.phone=res.data.phone
    form.college=res.data.college
    form.major=res.data.major
    form.className=res.data.className
    form.grade=res.data.grade
    form.dormBuilding=res.data.dormBuilding
    form.dormRoom=res.data.dormRoom
    form.carbonScore=res.data.carbonScore
    form.password=res.data.password
    dialogVisible11.value=true
  }
  
}
const submitEdit=async()=>{ 
  const params={
    stuNum: form.stuNum,
    name: form.name,
    gender: form.gender,
    idCard: form.idCard,
    phone: form.phone,
    college: form.college,
    major: form.major,
    className: form.className,
    grade: form.grade,
    dormBuilding: form.dormBuilding,
    dormRoom: form.dormRoom,
    carbonScore: form.carbonScore,
    password: form.password
  }
  const res=await studentUpdateService(params)
  if(res.code===1){
    ElMessage.success('修改成功')
    dialogVisible11.value=false
    page1()
  }else{
    ElMessage.error('修改失败')
  }
}

</script>
<template>
   <div class="S-search">
    <el-form :inline="true" :model="searchForm" class="demo-form-inline">
      <el-form-item label="姓名">
        <el-input 
          v-model="searchForm.name" 
          placeholder="请输入姓名"
          clearable
        ></el-input>
      </el-form-item>
     <el-form-item label="专业">
 <el-input 
          v-model="searchForm.major" 
          placeholder="请输入专业"
          clearable
        ></el-input>

</el-form-item>
      <el-form-item>
        <el-button type="success" @click="page1">搜索</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
  <el-button type="success" class="add-button" @click="openAddDialog">新增用户</el-button>
      <!-- 弹窗：新增用户表单 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增用户"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="form-container"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="formData.stuNum" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="formData.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio label="1">男</el-radio>
                <el-radio label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="formData.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学院" prop="college">
              <el-select v-model="formData.college" placeholder="请选择学院">
                <el-option label="计算机学院" value="计算机学院" />
                <el-option label="文学院" value="文学院" />
                <el-option label="理学院" value="理学院" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业" prop="major">
              <el-input v-model="formData.major" placeholder="请输入专业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="formData.className" placeholder="请输入班级（如2301）" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-select v-model="formData.grade" placeholder="请选择年级">
                <el-option label="大一" value="大一" />
                <el-option label="大二" value="大二" />
                <el-option label="大三" value="大三" />
                <el-option label="大四" value="大四" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="宿舍楼" prop="dormBuilding">
              <el-input v-model="formData.dormBuilding" placeholder="请输入宿舍楼（如A01）" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="宿舍号" prop="dormRoom">
              <el-input v-model="formData.dormRoom" placeholder="请输入宿舍号（如204）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="碳积分" prop="carbonScore">
              <el-input-number v-model="formData.carbonScore" :min="0" placeholder="碳积分" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 弹窗底部按钮 -->
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="loading">确定</el-button>
        </div>
      </template>
    </el-dialog>
    <el-table :data="tableData" style="width: 100%">
      <el-table-column prop="studentId" label="Id" width="50" />
    <el-table-column prop="stuNum" label="学号" width="105" />
      <el-table-column prop="name" label="姓名" width="85" />
      <el-table-column prop="gender" label="性别" width="55" />
      <el-table-column prop="idCard" label="身份证号" width="165" />
      <el-table-column prop="phone" label="手机号" width="110" />
      <el-table-column prop="password" label="密码" width="85" />
      <el-table-column prop="college" label="学院" width="110" />
      <el-table-column prop="major" label="专业" width="90" />
      <el-table-column prop="className" label="班级" width="80" />
      <el-table-column prop="grade" label="年级" width="80" />
      <el-table-column prop="dormBuilding" label="宿舍楼" width="80" />
      <el-table-column prop="dormRoom" label="宿舍号" width="80" />
      <el-table-column prop="carbonScore" label="碳积分" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
           <el-button type="primary" :icon="Edit" @click="findByid(scope.row.studentId)" circle />
          <el-button type="danger" :icon="Delete" @Click="deleteStudent(scope.row.studentId)" circle />
        </template>
      </el-table-column>
  </el-table>
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 30, 40]"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
        <el-dialog v-model="dialogVisible11" title="编辑学生信息" width="600px">
          <el-form :model="form"  >
            <el-form-item label="姓名" :label-width="formLabelWidth">
        <el-input v-model="form.name" autocomplete="off" />
      </el-form-item>
      <el-form-item label="学号" :label-width="formLabelWidth">
        <el-input v-model="form.stuNum" autocomplete="off" />
      </el-form-item>
      <el-form-item label="性别" :label-width="formLabelWidth">
        <el-radio-group v-model="form.gender">
          <el-radio label="1">男</el-radio>
          <el-radio label="0">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="身份证号" :label-width="formLabelWidth">
        <el-input v-model="form.idCard" autocomplete="off" />
      </el-form-item>
      <el-form-item label="手机号" :label-width="formLabelWidth">
        <el-input v-model="form.phone" autocomplete="off" />
      </el-form-item>
      <el-form-item label="密码" :label-width="formLabelWidth">
        <el-input v-model="form.password" autocomplete="off" />
      </el-form-item>
      <el-form-item label="学院" :label-width="formLabelWidth">
        <el-input v-model="form.college" autocomplete="off" />
      </el-form-item>
      <el-form-item label="专业" :label-width="formLabelWidth">
        <el-input v-model="form.major" autocomplete="off" />
      </el-form-item>
      <el-form-item label="班级" :label-width="formLabelWidth">
        <el-input v-model="form.className" autocomplete="off" />
      </el-form-item>
      <el-form-item label="年级" :label-width="formLabelWidth">
        <el-select v-model="form.grade" placeholder="请选择年级">
          <el-option label="大一" value="大一" />
          <el-option label="大二" value="大二" />
          <el-option label="大三" value="大三" />
          <el-option label="大四" value="大四" />
        </el-select>
      </el-form-item>
      <el-form-item label="宿舍楼" :label-width="formLabelWidth">
        <el-input v-model="form.dormBuilding" autocomplete="off" />
      </el-form-item>
      <el-form-item label="宿舍号" :label-width="formLabelWidth">
        <el-input v-model="form.dormRoom" autocomplete="off" />
      </el-form-item>
      <el-form-item label="碳积分" :label-width="formLabelWidth">
        <el-input-number v-model="form.carbonScore" :min="0" placeholder="碳积分" />
      </el-form-item>

    </el-form>

      <template #footer>
        <el-button @click="dialogVisible11 = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
</template>
<style scoped>
.container {
  padding: 20px;
}
.top-bar {
  margin-bottom: 20px;
  text-align: right;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.form-container {
  padding: 10px 0;
}
.gender-select{
  width: 150px;
}
.input-group {
  display: flex;
  align-items: center;
  gap: 1em;
}
.add-button {
  float: right;
}
</style>