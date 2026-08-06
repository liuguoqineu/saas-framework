package com.saas.framework.service;

import com.saas.framework.entity.SysDict;
import com.saas.framework.entity.SysDictItem;

import java.util.List;
import java.util.Map;

public interface DictService {

    SysDict getByCode(String code);

    List<SysDictItem> getItemsByDictCode(String code);

    List<SysDictItem> getItemsByParentValue(String code, String parentValue);

    Map<String, List<SysDictItem>> getDictMap(String code);

    Map<String, Object> getCustomerDicts();
}
