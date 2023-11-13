package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PermissionMapper;
import cc.vastsea.toyou.model.entity.Permission;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
	private static final Cache<Long, Set<Permission>> userPermissions = CaffeineFactory.newBuilder()
			.expireAfterWrite(3, TimeUnit.MINUTES)
			.build();
	@Resource
	private PermissionMapper permissionMapper;

	@Override
	public Set<Permission> getUserPermissions(long uid) {
		Set<Permission> permissions = userPermissions.getIfPresent(uid);
		if (permissions == null) {
			Permission permissionQuery = new Permission();
			QueryWrapper<Permission> queryWrapper = new QueryWrapper<>(permissionQuery);
			queryWrapper.eq("uid", uid);
			List<Permission> lps = permissionMapper.selectList(queryWrapper);
			permissions = Set.copyOf(lps);
			userPermissions.put(uid, permissions);
		}
		return permissions;
	}


	@Override
	public boolean checkPermission(long uid, String permission) {
		Set<Permission> permissions = getUserPermissions(uid);
		for (Permission p : permissions) {
			if (p.getPermission().equals("*")) {
				return true;
			}
			if (p.getPermission().equalsIgnoreCase(permission)) {
				if (p.getExpiry() == 0 || p.getExpiry() > System.currentTimeMillis()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Permission getPermission(long uid, String permission) {
		Set<Permission> permissions = getUserPermissions(uid);
		for (Permission p : permissions) {
			if (p.getPermission().equalsIgnoreCase(permission)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public void addPermission(long uid, String permission, long expiry) {
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission);
		if (permissionQuery == null) {
			permissionQuery = new Permission();
			permissionQuery.setUid(uid);
			permissionQuery.setPermission(permission);
			permissionQuery.setExpiry(expiry);
			boolean saveResult = this.save(permissionQuery);
			if (!saveResult) {
				throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误");
			}
		} else {
			permissionQuery.setExpiry(expiry);
			permissionMapper.updateById(permissionQuery);
		}
		userPermissions.invalidate(uid);
	}

	@Override
	public void removePermission(long uid, String permission) {
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission);
		if (permissionQuery != null) {
			permissionMapper.deleteById(permissionQuery.getId());
		}
		userPermissions.invalidate(uid);
	}

	@Override
	public void updatePermission(long uid, String permission, long expiry) {
		userPermissions.invalidate(uid);
		Permission permissionQuery = getPermission(uid, permission);
		if (permissionQuery != null) {
			permissionQuery.setExpiry(expiry);
			permissionMapper.updateById(permissionQuery);
		}
		userPermissions.invalidate(uid);
	}

	@Override
	public List<Permission> getGroups(long uid) {
		Set<Permission> permissions = getUserPermissions(uid);
		List<Permission> groups = new ArrayList<>();
		for (Permission permission : permissions) {
			if (permission.getExpiry() == 0 || permission.getExpiry() > System.currentTimeMillis()) {
				if (permission.getPermission().startsWith("group.")) {
					groups.add(permission);
				}
			}
		}
		return groups;
	}

	@Override
	public Group getMaxPriorityGroup(long uid) {
		List<Permission> groups = getGroups(uid);
		Group maxPriorityGroup = Group.DEFAULT;
		for (Permission group : groups) {
			try {
				Group g = Group.valueOf(group.getPermission().substring(6).toUpperCase());
				if (g.getPriority() > maxPriorityGroup.getPriority()) {
					maxPriorityGroup = g;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return maxPriorityGroup;
	}

	@Override
	public Permission getMaxPriorityGroupP(long uid){
		List<Permission> groups = getGroups(uid);
		Group maxPriorityGroup = Group.DEFAULT;
		Permission i = null;
		for (Permission group : groups) {
			try {
				Group g = Group.valueOf(group.getPermission().substring(6).toUpperCase());
				if (g.getPriority() > maxPriorityGroup.getPriority()) {
					maxPriorityGroup = g;
					i = group;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return i;
	}
}
