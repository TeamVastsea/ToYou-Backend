package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Set;

public interface PermissionService extends IService<Permission> {
	Set<Permission> getUserPermissions(long uid);

	boolean checkPermission(long uid, String permission);

	Permission getPermission(long uid, String permission);

	void addPermission(long uid, String permission, long expiry);

	void removePermission(long uid, String permission);

	void updatePermission(long uid, String permission, long expiry);
}
