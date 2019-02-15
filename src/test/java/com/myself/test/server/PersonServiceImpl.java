package com.myself.test.server;

import com.myself.test.client.Person;
import com.myself.test.client.PersonService;

import java.util.ArrayList;
import java.util.List;

/**
 * 类名称：PersonServiceImpl<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
@RpcService(PersonService.class)
public class PersonServiceImpl implements PersonService {
    @Override
    public List<Person> GetTestPerson(String name, int num) {
        List<Person> persons = new ArrayList<>(num);
        for (int i = 0; i < num; ++i) {
            persons.add(new Person(Integer.toString(i), name));
        }
        return persons;
    }
}
