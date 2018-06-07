
var toolbar = function(){
	return [{
		text : '环境',  
        iconCls : 'icon-control_add_blue',  
        handler : function(){
        	addBaseContainer();
        }
	},{
		text : '添加服务',  
        iconCls : 'icon-add',  
        handler : function(){
        	addPasService();
        }
	},{
		text : '公共服务',  
        iconCls : 'icon-add',  
        handler : function(){
        	copyMainServiceContainer();
        }
	},{
		text : '管家',  
        iconCls : 'icon-add',  
        handler : function(){
        	copyPaspmServiceContainer();
        }
	},{
		text : '前端',  
        iconCls : 'icon-web',  
        handler : function(){
        	addTomcatContainer();
        }
	},{
		text : '运行',  
        iconCls : 'icon-application_start',  
        handler : function(){
            startContainer();
        }
	},{
		text : '暂停',  
        iconCls : 'icon-control_pause_blue',  
        handler : function(){
        	pauseContainer();
        }
	},{
		text : '恢复',  
        iconCls : 'icon-control_repeat_blue',  
        handler : function(){
        	unpauseContainer();
        }
	},{
		text : '停止',  
        iconCls : 'icon-application_stop',  
        handler : function(){
        	stopContainer();
        }
	},{
		text : '重启',  
        iconCls : 'icon-arrow_rotate_clockwise',  
        handler : function(){
        	restartContainer();
        }
	},{
		text : '销毁',  
        iconCls : 'icon-control_remove_blue',  
        handler : function(){
            alert('销毁')
        }
	},{
		text : '查看日志',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	getContainerLog();
        }
	}];
}();