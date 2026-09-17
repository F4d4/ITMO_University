window.TRACES = window.TRACES || {};
window.TRACES["glitch"] = {
  meta: { scenario: "glitch" },
  regs: [
    {t:0,rw:"R",port:"D",reg:"LCKR",val:0,old:0},
    {t:0,rw:"R",port:"D",reg:"MODER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"MODER",val:0,old:0},
    {t:0,rw:"R",port:"D",reg:"OSPEEDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"OSPEEDR",val:0,old:0},
    {t:0,rw:"R",port:"D",reg:"OTYPER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"OTYPER",val:0,old:0},
    {t:0,rw:"R",port:"D",reg:"PUPDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"PUPDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"BSRR",val:8192,old:0},
    {t:0,rw:"W",port:"D",reg:"BSRR",val:536870912,old:8192},
    {t:0,rw:"W",port:"D",reg:"OSPEEDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"OTYPER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"PUPDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"MODER",val:67108864,old:0},
    {t:0,rw:"R",port:"D",reg:"MODER",val:67108864,old:67108864},
    {t:0,rw:"W",port:"D",reg:"MODER",val:0,old:67108864},
    {t:0,rw:"W",port:"D",reg:"OSPEEDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"OTYPER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"PUPDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"BSRR",val:8192,old:0},
    {t:0,rw:"R",port:"D",reg:"MODER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"MODER",val:67108864,old:0},
    {t:0,rw:"W",port:"D",reg:"OSPEEDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"OTYPER",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"PUPDR",val:0,old:0},
    {t:0,rw:"W",port:"D",reg:"BSRR",val:536870912,old:8192}
  ],
  pads: [
    {t:0,port:"D",pin:13,lvl:"0",note:"MODER"},
    {t:0,port:"D",pin:13,lvl:"Z",note:"MODER"},
    {t:0,port:"D",pin:13,lvl:"1",note:"MODER"},
    {t:0,port:"D",pin:13,lvl:"0",note:"BSRR"}
  ],
  app: [
    {t:0,msg:"правильный порядок: ODR загружается до включения каскада"},
    {t:0,msg:"наивный порядок: MODER пишется первым"}
  ]
};
