
var toolbar = function(){
	return [{
		text : '添加',  
        iconCls : 'icon-add',  
        handler : function(){
        	addDbDialog();
        	
        }
	},{
		text : '删除',  
        iconCls : 'icon-delete',  
        handler : function(){
        	
        	delDatanode();
        }
	},{
		text : '同步重启',  
        iconCls : 'icon-disk_upload',  
        handler : function(){
        	uploadConfig();
        }
	}];
}();