import request from '../util/request.js'
export const studentMealPageService=(params)=>{
return  request.get('/student/meal/page',{params})
};
export const studentMealWeekPageService=(params)=>{
  return  request.get('/student/meal/week/page',{params})
  };
export const studentMealMonthPageService=(params)=>{
  return  request.get('/student/meal/month/page',{params})
  };
  export const studentMealTermPageService=(params)=>{
  return  request.get('/student/meal/term/page',{params})
  };
  export const studentMealYearPageService=(params)=>{
  return  request.get('/student/meal/year/page',{params})
  };
export const weekXAxisService = (params) => {
return request.get('/student/period/year/week-carbon', { params })
}

export const monthCarbonService = (params) => {
return request.get('/student/period/year/month-carbon', { params })
}
