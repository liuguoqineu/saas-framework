package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色ID查询权限编码列表
     */
    @Select("SELECT p.code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID查询权限ID列表
     */
    @Select("SELECT rp.permission_id FROM sys_role_permission rp WHERE rp.role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色的所有权限关联
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色-权限关联
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_permission (role_id, permission_id) VALUES " +
            "<foreach collection='permissionIds' item='permId' separator=','>" +
            "(#{roleId}, #{permId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
