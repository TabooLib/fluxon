_list = [1,2,3]!!
print(&_list)
_sum = 0
for _i in 1..10 {
    _sum += &_i
}
_sum += (1..10)::sumOf(|| &it)
print(&_sum)

def func() {
    list2 = [4,5,6]!!
    print(&list2)
    sum2 = 0
    for i in 1..10 {
        sum2 += &i
    }
    sum2 += (1..10)::sumOf(|| &it)
    print(&sum2)
}