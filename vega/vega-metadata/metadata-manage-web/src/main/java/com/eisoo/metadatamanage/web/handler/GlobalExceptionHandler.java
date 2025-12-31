package com.eisoo.metadatamanage.web.handler;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import com.eisoo.standardization.common.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.eisoo.metadatamanage.web.util.CheckErrorVo;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.constant.Message;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理类，捕获异常，并根据异常返回错误信息。
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @PostConstruct
    public void init() {
        Result.setServiceName("MetadataManage");
    }

    /**
     * 处理鉴权失败异常
     */
    @ExceptionHandler(value = UnauthorizedException.class)
    @ResponseBody
    public ResponseEntity UnauthorizedExceptionHandler(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    /**
     * 处理自定义的业务异常，程序员主动抛出BizException异常会被这个方法捕获
     */
    @ExceptionHandler(value = AiShuException.class)
    @ResponseBody
    public ResponseEntity<Result> AsExceptionHandler(AiShuException e) {
        log.debug("发生异常:", e);
        return Result.error(e);
    }

    /**
     * POST请求体参数校验，使用hibernate-validator
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(BindException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.PARAMETER_FORMAT_INCORRECT, detail, Message.MESSAGE_PARAM_ERROR_SOLUTION));
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(ConstraintViolationException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.PARAMETER_FORMAT_INCORRECT, detail, Message.MESSAGE_PARAM_ERROR_SOLUTION));
    }

    @ExceptionHandler(value = MissingPathVariableException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MissingPathVariableException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.PARAMETER_FORMAT_INCORRECT, detail, Message.MESSAGE_PATHVARIABLE_ERROR_SOLUTION));
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MaxUploadSizeExceededException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.PARTAIL_FAILURE, detail, Message.MESSAGE_MaxUploadSizeExceededException));
    }

    @ExceptionHandler(value = MultipartException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MultipartException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.PARTAIL_FAILURE, detail, Message.MESSAGE_MultipartException));
    }

    /**
     * Query 参数（Request Parameter） 校验
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MissingServletRequestParameterException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        String solution = Message.MESSAGE_PARAM_ERROR_SOLUTION;
        return Result.error(new AiShuException(ErrorCodeEnum.MethodArgumentTypeMismatchException,
                detail, solution));
    }

    /**
     * part 参数（request part） 校验
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = MissingServletRequestPartException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MissingServletRequestPartException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        String solution = Message.MESSAGE_PARAM_ERROR_SOLUTION;
        return Result.error(new AiShuException(ErrorCodeEnum.MethodArgumentTypeMismatchException,
                detail, solution));
    }


    /**
     * 处理请求体为空或者参数不正确异常
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(HttpMessageNotReadableException e) {
        log.debug("发生异常:", e);
        String solution = Message.MESSAGE_PARAM_ERROR_SOLUTION;
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException errorEntity = (InvalidFormatException) cause;
            List<Reference> pathList = errorEntity.getPath();
            List<CheckErrorVo> detail = Lists.newArrayList();
            detail.add(new CheckErrorVo(pathList.get(0).getFieldName(), String.format("%s的值[%s]不符合接口要求", pathList.get(0).getFieldName(), errorEntity.getValue())));
            return Result.error(
                    new AiShuException(ErrorCodeEnum.InvalidParameter, "请求体格式不正确", detail, solution));
        }
        return Result.error(new AiShuException(ErrorCodeEnum.InvalidParameter, "请求体为空或格式不正确", null, solution));
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(MethodArgumentTypeMismatchException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        String solution = Message.MESSAGE_PARAM_ERROR_SOLUTION;
        return Result.error(new AiShuException(ErrorCodeEnum.MethodArgumentTypeMismatchException,
                detail, solution));
    }


    /**
     * 其他运行时异常
     */
    @ExceptionHandler(value = RuntimeException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(RuntimeException e) {
        log.warn("发生异常,原因是:", e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR, new AiShuException(ErrorCodeEnum.InternalError), null);
    }

    @ExceptionHandler(value = NoHandlerFoundException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(NoHandlerFoundException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        String solution = Message.MESSAGE_PARAM_ERROR_SOLUTION;
        return Result.error(new AiShuException(ErrorCodeEnum.MethodArgumentTypeMismatchException,
                detail, solution));
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.debug("发生异常:", e);
        String solution = String.format("当前请求方式为：%s，支持的接口请求方式有：%s。详细信息参见产品 API 文档。", e.getMethod(), e.getSupportedHttpMethods());
        return Result.error(new AiShuException(ErrorCodeEnum.HttpRequestMethodNotSupportedException, null, solution));
    }

    @ExceptionHandler(value = org.springframework.dao.DuplicateKeyException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(org.springframework.dao.DuplicateKeyException e) {
        log.debug("发生异常:", e);
        List<CheckErrorVo> detail = getErrorDetailInfo(e);
        return Result.error(new AiShuException(ErrorCodeEnum.Duplicated, detail, Message.MESSAGE_Duplicated_SOLUTION));
    }

    @ExceptionHandler(value = org.springframework.dao.DeadlockLoserDataAccessException.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(org.springframework.dao.DeadlockLoserDataAccessException e) {
        log.debug("发生异常:", e);
        return Result.error(new AiShuException(ErrorCodeEnum.SystemBusy, null, Message.SYSTEM_BUSY_ERROR));
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<Result> exceptionHandler(Exception e) {
        log.error("发生异常:", e);
        return Result.error(new AiShuException(ErrorCodeEnum.UnKnowException));
    }

    /**
     * 根据异常类型不同，返回不同的异常细节描述
     */
    private List<CheckErrorVo> getErrorDetailInfo(Exception e) {
        List<CheckErrorVo> detail = Lists.newArrayList();
        String key = "";
        String message = "";
        if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException errorEntity = (MethodArgumentTypeMismatchException) e;
            key = errorEntity.getName();
            message = String.format("参数%s的值%s与接口要求类型不匹配", errorEntity.getName(), errorEntity.getValue());
            detail.add(new CheckErrorVo(key, message));
        } else if (e instanceof NoHandlerFoundException) {
            NoHandlerFoundException errorEntity = (NoHandlerFoundException) e;
            key = errorEntity.getRequestURL();
            message = "未找到该URL对应的处理方法";
            detail.add(new CheckErrorVo(key, message));
        } else if (e instanceof BindException) {
            List<FieldError> allErrors = ((BindException) e).getBindingResult().getFieldErrors();
            for (FieldError errorEntity : allErrors) {
                key = AiShuUtil.camelToUnderline(errorEntity.getField());
                message = errorEntity.getDefaultMessage();
                detail.add(new CheckErrorVo(key, message));
            }
        } else if (e instanceof ConstraintViolationException) {
            Set<ConstraintViolation<?>> allErrors = ((ConstraintViolationException) e).getConstraintViolations();
            for (ConstraintViolation errorEntity : allErrors) {
                String path = errorEntity.getPropertyPath().toString();
                path = path.substring(path.indexOf("."), path.length());
                key = AiShuUtil.camelToUnderline(path);
                message = errorEntity.getMessage();
                detail.add(new CheckErrorVo(key, message));
            }
        } else if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException errorEntity = (MissingServletRequestParameterException) e;
            key = errorEntity.getParameterName();
            message = Message.MESSAGE_INPUT_NOT_EMPTY;
            detail.add(new CheckErrorVo(key, message));
        } else if (e instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException errorEntity = (MissingServletRequestPartException) e;
            key = errorEntity.getRequestPartName();
            message = Message.MESSAGE_INPUT_NOT_EMPTY;
            detail.add(new CheckErrorVo(key, message));
        } else if (e instanceof org.springframework.dao.DuplicateKeyException) {
            key = ErrorCodeEnum.Duplicated.getErrorCode();
            message = ErrorCodeEnum.Duplicated.getErrorMsg();
            detail.add(new CheckErrorVo(key, message));
        } else if (e instanceof org.springframework.dao.DeadlockLoserDataAccessException) {
            key = ErrorCodeEnum.DeadlockException.getErrorCode();
            message = ErrorCodeEnum.DeadlockException.getErrorMsg();
            detail.add(new CheckErrorVo(key, message));
        }
        return detail;
    }

    public static AiShuException getNewAishuException(ErrorCodeEnum errorCodeEnum, String detail,String solution) {
        List<CheckErrorVo> errorVos = Lists.newArrayList();
        String key = errorCodeEnum.getErrorCode();
        String message = detail;
        errorVos.add(new CheckErrorVo(key, message));
        return  new AiShuException(errorCodeEnum, errorVos, solution);
    }

    public static AiShuException getNewAishuException(ErrorCodeEnum errorCodeEnum, String detail) {
        return getNewAishuException(errorCodeEnum, detail, Message.MESSAGE_PARAM_ERROR_SOLUTION);
    }
    public static AiShuException getNewAishuException(ErrorCodeEnum errorCodeEnum) {
        return getNewAishuException(errorCodeEnum, errorCodeEnum.getErrorMsg());
    }

    public static AiShuException getNewAishuException(String code, String description, String detail, String solution) {
        List<CheckErrorVo> errorVos = Lists.newArrayList();
        String key = code;
        String message = detail;
        errorVos.add(new CheckErrorVo(key, message));
        return new AiShuException(code, description, errorVos, solution);
    }
}
