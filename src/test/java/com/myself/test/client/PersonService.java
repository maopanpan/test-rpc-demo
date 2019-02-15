package com.myself.test.client;

import java.util.List;

/**
 * 类名称：PersonService<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public interface PersonService {
    List<Person> GetTestPerson(String name, int num);
}
