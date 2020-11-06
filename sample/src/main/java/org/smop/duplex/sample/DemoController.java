package org.smop.duplex.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class DemoController {
    
    @Autowired
    private DemoService demoService;
    
    @RequestMapping("check")
    public Integer checkSome() {
        ArrayList<String> tagTuple = new ArrayList<>();
        tagTuple.add("bar");
        tagTuple.add("foo");
        demoService.checkSome(tagTuple);
        return 0;
    }
}
