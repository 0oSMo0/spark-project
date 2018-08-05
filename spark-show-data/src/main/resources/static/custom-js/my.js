/**
 * 清空原来的元素
 */
function removeEle() {
    $("#tHead").empty();
    $("#tBody").empty();
}

/**
 * 用户session分析模块：
 * session聚合统计(访问时长,访问步长)
 */
function loadSessionAggrStat() {
    removeEle();
    $.ajax({
        type: 'POST',
        url: 'querySessionAggrStat',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            tBody += '<tr class="text-c">'
                + '<td>任务ID</td>'
                + '<td>' + data[0].taskId + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>session数量</strong></td>'
                + '<td>' + data[0].session_count + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>1-3s访问时长的比率</td>'
                + '<td>' + data[0].visit_length_1s_3s_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>4-6s访问时长的比率</td>'
                + '<td>' + data[0].visit_length_4s_6s_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>7-9s访问时长的比率</td>'
                + '<td>' + data[0].visit_length_7s_9s_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>10-30s访问时长的比率</td>'
                + '<td>' + data[0].visit_length_10s_30s_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>30-60s访问时长的比率</td>'
                + '<td>' + data[0].visit_length_30s_60s_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>1-3min访问时长的比率</td>'
                + '<td>' + data[0].visit_length_1m_3m_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>3-10min访问时长的比率</td>'
                + '<td>' + data[0].visit_length_3m_10m_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>10-30min访问时长的比率</td>'
                + '<td>' + data[0].visit_length_10m_30m_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>大于30min访问时长的比率</td>'
                + '<td>' + data[0].visit_length_30m_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>1-3访问步长的比率</td>'
                + '<td>' + data[0].step_length_1_3_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>4-6访问步长的比率</td>'
                + '<td>' + data[0].step_length_4_6_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>7-9访问步长的比率</td>'
                + '<td>' + data[0].step_length_7_9_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>10-30访问步长的比率</td>'
                + '<td>' + data[0].step_length_10_30_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>30-60访问步长的比率</td>'
                + '<td>' + data[0].step_length_30_60_ratio + '</td>'
                + '</tr>';
            tBody += '<tr class="text-c">'
                + '<td>大于60访问步长的比率</td>'
                + '<td>' + data[0].step_length_60_ratio + "" + '</td>'
                + '</tr>';
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 用户session分析模块：
 * top10热门品类
 */
function loadTop10Category() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">任务ID</th>' +
        '<th width="80">品类ID</th>' +
        '<th width="80">点击次数</th>' +
        '<th width="80">下单次数</th>' +
        '<th width="80">支付次数</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryTop10Category',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].taskId + '</td>'
                    + '<td>' + data[i].categoryId + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '<td>' + data[i].orderCount + '</td>'
                    + '<td>' + data[i].payCount + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 用户session分析模块：
 * top10热门品类的top10点击次数的session
 */
function loadTop10Session() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">任务ID</th>' +
        '<th width="80">品类ID</th>' +
        '<th width="80">sessionID</th>' +
        '<th width="80">点击次数</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryTop10Session',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].taskId + '</td>'
                    + '<td>' + data[i].categoryId + '</td>'
                    + '<td>' + data[i].sessionId + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 页面单跳转化率模块
 */
function loadPageOneStepConvertRate() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">任务ID</th>' +
        '<th width="160">页面单跳转化率</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryPageSplitConvertRate',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].taskId + '</td>'
                    + '<td>' + data[i].convertRate + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 各区域top3热门商品统计模块
 */
function loadAreaTop3Product() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">任务ID</th>' +
        '<th width="80">地区</th>' +
        '<th width="80">地区水平</th>' +
        '<th width="80">产品ID</th>' +
        '<th width="80">城市信息</th>' +
        '<th width="80">点击次数</th>' +
        '<th width="80">产品名称</th>' +
        '<th width="80">产品状态</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryAreaTop3Product',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].taskId + '</td>'
                    + '<td>' + data[i].area + '</td>'
                    + '<td>' + data[i].areaLevel + '</td>'
                    + '<td>' + data[i].productId + '</td>'
                    + '<td>' + data[i].cityInfos + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '<td>' + data[i].productName + '</td>'
                    + '<td>' + data[i].productStatus + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 广告点击流量实时统计模块之
 * 每天各省各市各广告的点击次数
 */
function loadAdStat() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">日期</th>' +
        '<th width="80">省份</th>' +
        '<th width="80">城市</th>' +
        '<th width="80">广告ID</th>' +
        '<th width="80">点击次数</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryAdStat',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].date + '</td>'
                    + '<td>' + data[i].province + '</td>'
                    + '<td>' + data[i].city + '</td>'
                    + '<td>' + data[i].adId + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 广告点击流量实时统计模块之
 * 每天各省份的top3热门广告
 */
function loadAdProvinceTop3() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">日期</th>' +
        '<th width="80">省份</th>' +
        '<th width="80">广告ID</th>' +
        '<th width="80">点击次数</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryAdProvinceTop3',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].date + '</td>'
                    + '<td>' + data[i].province + '</td>'
                    + '<td>' + data[i].adId + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}

/**
 * 广告点击流量实时统计模块之
 * 计算最近1小时滑动窗口的广告点击趋势
 */
function loadAdClickTrend() {
    removeEle();
    var tHead = '<tr class="text-c">' +
        '<th width="80">日期</th>' +
        '<th width="80">小时</th>' +
        '<th width="80">分钟</th>' +
        '<th width="80">广告ID</th>' +
        '<th width="80">点击次数</th></tr>';
    $('#tHead').append(tHead);
    $.ajax({
        type: 'POST',
        url: 'queryAdClickTrend',
        dataType: 'json',
        success: function (data) {
            var tBody = '';
            for (var i = 0; i < data.length; i++) {
                tBody += '<tr class="text-c">'
                    + '<td>' + data[i].date + '</td>'
                    + '<td>' + data[i].hour + '</td>'
                    + '<td>' + data[i].minute + '</td>'
                    + '<td>' + data[i].adId + '</td>'
                    + '<td>' + data[i].clickCount + '</td>'
                    + '</tr>';
            }
            $('#tHead').append(tBody);
        }
    });
}