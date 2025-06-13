/**
 * ES2025 特性演示
 * 
 * 本文件展示了ES2025中的一些主要新特性
 * 注意：这些特性尚未在所有JavaScript环境中得到完全支持
 */

// ===== 1. Object.groupBy() 和 Map.groupBy() =====

/**
 * Object.groupBy() 将数组元素按照指定条件分组到一个对象中
 * Map.groupBy() 类似，但返回Map对象，可以使用对象作为键
 */

// 示例数据
const people = [
  { name: '张三', age: 25, city: '北京' },
  { name: '李四', age: 32, city: '上海' },
  { name: '王五', age: 28, city: '北京' },
  { name: '赵六', age: 42, city: '广州' },
  { name: '钱七', age: 35, city: '上海' }
];

// 使用Object.groupBy按城市分组
try {
  const byCity = Object.groupBy(people, person => person.city);
  console.log('按城市分组:', byCity);
  // 输出: { '北京': [{...}, {...}], '上海': [{...}, {...}], '广州': [{...}] }
} catch (error) {
  console.log('Object.groupBy尚未在此环境中支持:', error.message);
  
  // 兼容实现
  function objectGroupBy(array, keyFn) {
    return array.reduce((result, item) => {
      const key = keyFn(item);
      if (!result[key]) {
        result[key] = [];
      }
      result[key].push(item);
      return result;
    }, {});
  }
  
  const byCity = objectGroupBy(people, person => person.city);
  console.log('按城市分组 (兼容实现):', byCity);
}

// 使用Map.groupBy按年龄段分组
try {
  const ageGroups = { young: '年轻人', middle: '中年人', senior: '老年人' };
  
  const byAgeGroup = Map.groupBy(people, person => {
    if (person.age < 30) return ageGroups.young;
    if (person.age < 40) return ageGroups.middle;
    return ageGroups.senior;
  });
  
  console.log('按年龄段分组:');
  for (const [group, members] of byAgeGroup.entries()) {
    console.log(`- ${group}: ${members.map(p => p.name).join(', ')}`);
  }
} catch (error) {
  console.log('Map.groupBy尚未在此环境中支持:', error.message);
  
  // 兼容实现
  function mapGroupBy(array, keyFn) {
    return array.reduce((result, item) => {
      const key = keyFn(item);
      if (!result.has(key)) {
        result.set(key, []);
      }
      result.get(key).push(item);
      return result;
    }, new Map());
  }
  
  const ageGroups = { young: '年轻人', middle: '中年人', senior: '老年人' };
  
  const byAgeGroup = mapGroupBy(people, person => {
    if (person.age < 30) return ageGroups.young;
    if (person.age < 40) return ageGroups.middle;
    return ageGroups.senior;
  });
  
  console.log('按年龄段分组 (兼容实现):');
  for (const [group, members] of byAgeGroup.entries()) {
    console.log(`- ${group}: ${members.map(p => p.name).join(', ')}`);
  }
}

// ===== 2. Promise.withResolvers() =====

/**
 * Promise.withResolvers() 提供了一种更简洁的方式来创建可控的Promise
 * 它返回一个包含promise、resolve和reject的对象
 */

async function demoPromiseWithResolvers() {
  console.log('\n=== Promise.withResolvers() 演示 ===');
  
  try {
    // 尝试使用原生API
    const { promise, resolve, reject } = Promise.withResolvers();
    
    // 模拟异步操作
    setTimeout(() => {
      console.log('操作完成，解析Promise');
      resolve('成功结果');
    }, 1000);
    
    console.log('等待Promise解析...');
    const result = await promise;
    console.log('Promise已解析:', result);
    
  } catch (error) {
    console.log('Promise.withResolvers尚未在此环境中支持:', error.message);
    
    // 兼容实现
    function promiseWithResolvers() {
      let resolve, reject;
      const promise = new Promise((res, rej) => {
        resolve = res;
        reject = rej;
      });
      return { promise, resolve, reject };
    }
    
    const { promise, resolve, reject } = promiseWithResolvers();
    
    // 模拟异步操作
    setTimeout(() => {
      console.log('操作完成，解析Promise (兼容实现)');
      resolve('成功结果');
    }, 1000);
    
    console.log('等待Promise解析... (兼容实现)');
    const result = await promise;
    console.log('Promise已解析 (兼容实现):', result);
  }
}

// 运行Promise.withResolvers演示
demoPromiseWithResolvers();

// ===== 3. 实际应用示例 =====

/**
 * 实际应用：使用Promise.withResolvers实现一次性事件
 */
function createOneTimeEvent() {
  try {
    const { promise, resolve, reject } = Promise.withResolvers();
    return { 
      trigger: resolve, 
      cancel: reject, 
      wait: () => promise 
    };
  } catch (error) {
    // 兼容实现
    let resolve, reject;
    const promise = new Promise((res, rej) => {
      resolve = res;
      reject = rej;
    });
    return { 
      trigger: resolve, 
      cancel: reject, 
      wait: () => promise 
    };
  }
}

async function demoOneTimeEvent() {
  console.log('\n=== 一次性事件演示 ===');
  
  const event = createOneTimeEvent();
  
  // 在另一个上下文中等待事件
  setTimeout(async () => {
    console.log('开始等待事件...');
    try {
      const data = await event.wait();
      console.log('事件已触发，收到数据:', data);
    } catch (error) {
      console.log('事件已取消:', error);
    }
  }, 0);
  
  // 模拟延迟后触发事件
  setTimeout(() => {
    console.log('触发事件');
    event.trigger({ message: '这是事件数据' });
  }, 2000);
}

// 运行一次性事件演示
demoOneTimeEvent();

/**
 * 实际应用：使用Object.groupBy进行数据分析
 */
async function demoDataAnalysis() {
  console.log('\n=== 数据分析演示 ===');
  
  // 模拟从API获取的用户数据
  const users = [
    { id: 1, name: '张三', age: 25, role: 'admin', active: true },
    { id: 2, name: '李四', age: 32, role: 'user', active: true },
    { id: 3, name: '王五', age: 28, role: 'user', active: false },
    { id: 4, name: '赵六', age: 42, role: 'admin', active: true },
    { id: 5, name: '钱七', age: 35, role: 'editor', active: true },
    { id: 6, name: '孙八', age: 22, role: 'user', active: false }
  ];
  
  try {
    // 按角色分组
    const byRole = Object.groupBy(users, user => user.role);
    console.log('用户按角色分组:');
    for (const [role, usersInRole] of Object.entries(byRole)) {
      console.log(`- ${role}: ${usersInRole.length}人`);
    }
    
    // 按活跃状态分组
    const byActiveStatus = Object.groupBy(users, user => user.active ? '活跃' : '非活跃');
    console.log('\n用户按活跃状态分组:');
    for (const [status, usersWithStatus] of Object.entries(byActiveStatus)) {
      console.log(`- ${status}: ${usersWithStatus.length}人`);
    }
    
  } catch (error) {
    console.log('使用兼容实现...');
    
    // 兼容实现
    function objectGroupBy(array, keyFn) {
      return array.reduce((result, item) => {
        const key = keyFn(item);
        if (!result[key]) {
          result[key] = [];
        }
        result[key].push(item);
        return result;
      }, {});
    }
    
    // 按角色分组
    const byRole = objectGroupBy(users, user => user.role);
    console.log('用户按角色分组:');
    for (const [role, usersInRole] of Object.entries(byRole)) {
      console.log(`- ${role}: ${usersInRole.length}人`);
    }
    
    // 按活跃状态分组
    const byActiveStatus = objectGroupBy(users, user => user.active ? '活跃' : '非活跃');
    console.log('\n用户按活跃状态分组:');
    for (const [status, usersWithStatus] of Object.entries(byActiveStatus)) {
      console.log(`- ${status}: ${usersWithStatus.length}人`);
    }
  }
}

// 运行数据分析演示
demoDataAnalysis();

// 注意：由于这些是新特性，您可能需要使用最新版本的Node.js或现代浏览器才能运行
console.log('\n注意：这些特性是ES2025的一部分，可能尚未在所有JavaScript环境中得到支持。');
console.log('本示例包含了兼容实现，以便在不支持的环境中也能展示功能。'); 