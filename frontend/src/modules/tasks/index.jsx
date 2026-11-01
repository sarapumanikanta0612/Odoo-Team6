import{CheckSquareOutlined}from'@ant-design/icons'
import{SECTIONS}from'../../app/moduleSections'
import{ROLES}from'../../auth/roles'
import MyTasksPage from'./MyTasksPage'
export const tasksModule={key:'tasks',label:'My tasks',description:'Assigned work and status updates.',icon:<CheckSquareOutlined/>,section:SECTIONS.OPERATIONS,path:'tasks',roles:[ROLES.EMPLOYEE],order:20,routes:[{index:true,element:<MyTasksPage/>}]}
export default tasksModule
