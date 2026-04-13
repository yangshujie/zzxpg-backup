def algsMain(*args, **kwargs):
    try:
        print('比值量化输出函数开始量化')
        rst = args[0]
        replaceList(rst, 'null', None)
        config = kwargs["config"]
        k = eval(config['Kk'])
        b = eval(config['b'])
        result_list_new = rst.copy()
        for res in rst:
            if res is None:
                result_list_new.append(1)
            else:
                result_list_new.append(k * res + b)
        return_data = {
            "mapRange": '无',
            "value": result_list_new
        }
        print("比值量化输出函数已计算完成")
        return True, return_data
    except Exception as e:
        return False, e.args

def replaceList(inputList, pre, current):
    for index, item in enumerate(inputList):
        if item == pre:
            inputList[index] = current


if __name__ == "__main__":
    a = [0.9984, 0.844, 0.547]
    print(algsMain(a))
