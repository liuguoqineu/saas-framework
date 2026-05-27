package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.entity.SysDict;
import com.saas.framework.entity.SysDictItem;
import com.saas.framework.mapper.SysDictMapper;
import com.saas.framework.mapper.SysDictItemMapper;
import com.saas.framework.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DictServiceImpl implements DictService {

    @Resource
    private SysDictMapper sysDictMapper;

    @Resource
    private SysDictItemMapper sysDictItemMapper;

    @Override
    public SysDict getByCode(String code) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getCode, code);
        return sysDictMapper.selectOne(wrapper);
    }

    @Override
    public List<SysDictItem> getItemsByDictCode(String code) {
        SysDict dict = getByCode(code);
        if (dict == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictItem::getDictId, dict.getId())
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort);
        return sysDictItemMapper.selectList(wrapper);
    }

    @Override
    public List<SysDictItem> getItemsByParentValue(String code, String parentValue) {
        SysDict dict = getByCode(code);
        if (dict == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictItem::getDictId, dict.getId())
                .eq(SysDictItem::getParentValue, parentValue)
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort);
        return sysDictItemMapper.selectList(wrapper);
    }

    @Override
    public Map<String, List<SysDictItem>> getDictMap(String code) {
        List<SysDictItem> items = getItemsByDictCode(code);

        Map<String, List<SysDictItem>> result = new HashMap<>();
        for (SysDictItem item : items) {
            String parentValue = item.getParentValue();
            if (parentValue == null || parentValue.isEmpty()) {
                continue;
            }
            result.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(item);
        }

        return result;
    }

    @Override
    public Map<String, Object> getCustomerDicts() {
        Map<String, Object> result = new HashMap<>();

        result.put("businessCategory", getItemsByDictCode("business_category"));

        List<SysDictItem> businessTypeItems = getItemsByDictCode("business_type");
        Map<String, List<SysDictItem>> businessTypeMap = new HashMap<>();
        for (SysDictItem item : businessTypeItems) {
            String parentValue = item.getParentValue();
            if (parentValue != null && !parentValue.isEmpty()) {
                businessTypeMap.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(item);
            }
        }
        result.put("businessTypeMap", businessTypeMap);

        result.put("cooperationCategory", getItemsByDictCode("cooperation_category"));

        List<SysDictItem> cooperationStatusItems = getItemsByDictCode("cooperation_status");
        Map<String, List<SysDictItem>> cooperationStatusMap = new HashMap<>();
        for (SysDictItem item : cooperationStatusItems) {
            String parentValue = item.getParentValue();
            if (parentValue != null && !parentValue.isEmpty()) {
                cooperationStatusMap.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(item);
            }
        }
        result.put("cooperationStatusMap", cooperationStatusMap);

        result.put("maintenanceCategory", getItemsByDictCode("maintenance_category"));

        result.put("gasScale", getItemsByDictCode("gas_scale"));

        return result;
    }
}
