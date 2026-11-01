import{App,Select,Table}from'antd'
import ApiErrorAlert from'../../components/ApiErrorAlert'
import PageHeader from'../../components/PageHeader'
import{useApiQuery}from'../../hooks/useApiQuery'
import{api}from'../../services/apiClient'
import{formatDate,humanizeEnum}from'../../utils/format'
const S=['PENDING','IN_PROGRESS','COMPLETED','BLOCKED']
export default function MyTasksPage(){const{message}=App.useApp(),q=useApiQuery(signal=>api.get('/api/tasks/mine',{signal}),[]);const update=async(id,status)=>{try{await api.patch(`/api/tasks/${id}/status`,{status});message.success('Task updated.');q.reload()}catch(e){message.error(e.message)}};return <><PageHeader eyebrow="Employee work" title="My tasks" subtitle="Only tasks assigned to your employee profile are shown."/>{q.error?<ApiErrorAlert error={q.error} onRetry={q.reload}/>:<section className="wams-panel"><Table rowKey="id" loading={q.loading} dataSource={q.data??[]} columns={[{title:'Task',render:(_,r)=><div><strong>{r.title}</strong><div className="wams-resource-summary">{r.projectName}{r.functionName?` · ${r.functionName}`:''}</div></div>},{title:'Due',dataIndex:'dueDate',render:formatDate},{title:'Status',dataIndex:'status',render:(v,r)=><Select value={v} style={{width:145}} options={S.map(x=>({value:x,label:humanizeEnum(x)}))} onChange={x=>update(r.id,x)}/> }]} scroll={{x:600}}/></section>}</>}
