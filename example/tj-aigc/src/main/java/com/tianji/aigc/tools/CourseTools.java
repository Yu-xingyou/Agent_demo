package com.tianji.aigc.tools;


import com.tianji.aigc.Constant.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourseTools {
    private final CourseClient courseClient;

    /**
     *
     * @param courseId
     * @return
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID)Long courseId){
        return Optional.ofNullable(courseId)
                .map(id -> this.courseClient.baseInfo(id,true))
                .map(CourseInfo::of)
                .orElse(null);

    }
}
