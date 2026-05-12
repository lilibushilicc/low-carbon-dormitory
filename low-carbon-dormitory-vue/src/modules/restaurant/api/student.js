import request from '../util/request.js'
export const studentPageService=(params)=>{
return  request.get('/student/page',{params})
};
export const studentAddService=(params)=>{
return  request.post('/student/add',params)
};
export const studentDeleteService=(params)=>{
return  request.delete('/student/delete',{params:params});
};
export const studentFindService=(params)=>{
return  request.get('/student/id',{params:params});
};
export const studentUpdateService=(params)=>{
return  request.put('/student/update',params)
}